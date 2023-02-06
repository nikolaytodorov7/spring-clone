package spring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.io.Resources;
import spring.annotation.*;
import spring.dic.ApplicationContext;
import spring.dic.ApplicationContextException;
import spring.exception.BeanCreationException;
import spring.server.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassInjector {
    private static final String PROJECT_LOAD_FOLDER = "src\\main\\java\\"; //todo change folder to look for project when jar ...
    private static final Pattern PATH_PARAMS_PATTERN = Pattern.compile("\\{\\w+}");
    private static final String REPLACEMENT_PATTERN_STR = "[\\\\w]+";

    private final Set<Class<?>> mapperClasses = new HashSet<>();
    private final MappersConfig config = new MappersConfig();
    private final Set<Class<?>> classes = new HashSet<>();
    private final Class<?> primaryClass;
    private ApplicationContext applicationContext;
    private DispatcherServlet dispatcherServlet;

    public ClassInjector(Class<?> primaryClass) {
        this.primaryClass = primaryClass;
        createContainerWithProperties();
        try {
            dispatcherServlet = applicationContext.getInstance(DispatcherServlet.class);
        } catch (Exception ignored) { // DispatcherServlet will be created everytime
        }
    }

    private void createContainerWithProperties() {
        Properties properties;
        try {
            properties = Resources.getResourceAsProperties("application.properties");
        } catch (IOException e) {
            throw new RuntimeException("No properties file found!"); // todo check original ex
        }

        applicationContext = new ApplicationContext(properties);
    }

    public void injectClasses() throws BeanCreationException {
        getClasses(primaryClass);
        registerInterfacesImplementation();
        config.configure(applicationContext, mapperClasses);
        registerClasses();
    }

    private void getClasses(Class<?> c) {
        String classPath = c.getCanonicalName();
        String className = c.getSimpleName();
        int trimIndex = classPath.lastIndexOf('.' + className);
        String projectNameFolder = classPath.substring(0, trimIndex);
        File dir = new File(PROJECT_LOAD_FOLDER + projectNameFolder);
        getClasses(dir);
    }

    private void getClasses(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            processFile(f);
        }
    }

    private void processFile(File f) {
        if (f.isDirectory()) {
            getClasses(f);
            return;
        }

        String classStr = f.toString();
        if (f.isFile() && classStr.endsWith(".java")) { // todo work with compiled - .class
            String className = classStr
                    .substring(PROJECT_LOAD_FOLDER.length(), classStr.lastIndexOf(".java")) // todo work with compiled - .class
                    .replace("\\", ".");

            Class<?> c;
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
                return;
            }

            if (c.isAnnotationPresent(Mapper.class) && c.isInterface())
                mapperClasses.add(c);
            else
                classes.add(c);
        }
    }

    private void registerInterfacesImplementation() throws ApplicationContextException {
        for (Class<?> clazz : classes) {
            if (clazz.isInterface())
                continue;

            Arrays.stream(clazz.getInterfaces()).forEach((i) -> applicationContext.registerImplementation(i, clazz));
        }
    }

    private void registerClasses() throws BeanCreationException {
        for (Class<?> c : classes) {
            if (c.isInterface())
                continue;

            inject(c);
        }
    }

    private void inject(Class<?> c) throws BeanCreationException {
        Configuration configuration = c.getDeclaredAnnotation(Configuration.class);
        if (configuration != null) {
            extractBeansFromConfig(c);
            return;
        }

        if (!isComponentInstance(c))
            return;

        Object classInstance = applicationContext.getInstance(c);
        RestController restController = c.getDeclaredAnnotation(RestController.class);
        RequestMapping requestMapping = c.getDeclaredAnnotation(RequestMapping.class);
        if (restController == null && requestMapping == null)
            return;

        String mapping = requestMapping.value() != null ? requestMapping.value()[0] : "";
        processFields(c, classInstance);
        processMethods(c, classInstance, mapping);
    }

    private void extractBeansFromConfig(Class<?> c) {
        Method[] methods = c.getDeclaredMethods();
        Object instance = applicationContext.getInstance(c);
        for (Method method : methods) {
            Bean beanAnnotation = method.getDeclaredAnnotation(Bean.class);
            if (beanAnnotation == null)
                continue;

            try {
                Object invoked = method.invoke(method, instance);
                applicationContext.registerInstance(invoked);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e); // todo
            }
        }
    }

    private void processFields(Class<?> c, Object instance) {
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Autowired autowired = field.getDeclaredAnnotation(Autowired.class);
            if (autowired == null)
                continue;

            try {
                Object o = applicationContext.getInstance(field.getType());
                field.set(instance, o);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e); // todo
            }
        }
    }

    private void processMethods(Class<?> c, Object instance, String mapping) throws BeanCreationException {
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method method : declaredMethods) {
            method.setAccessible(true);
            Annotation[] methodAnnotations = method.getDeclaredAnnotations();
            processMethodAnnotations(instance, mapping, method, methodAnnotations);
        }
    }

    void defaultMethod() { // to avoid default in switch
    }

    private void processMethodAnnotations(Object instance, String mapping, Method method, Annotation[] annotations) throws
            BeanCreationException {
        for (Annotation annotation : annotations) {
            switch (annotation) {
                case GetMapping getMapping -> putMappings("GET", instance, mapping, method, getMapping.value());
                case PostMapping postMapping -> putMappings("POST", instance, mapping, method, postMapping.value());
                case PutMapping putMapping -> putMappings("PUT", instance, mapping, method, putMapping.value());
                case DeleteMapping delMapping -> putMappings("DELETE", instance, mapping, method, delMapping.value());
                default -> defaultMethod();
            }
        }
    }

    private void putMappings(String requestMethod, Object instance, String mapping, Method method, String[] paths) throws
            BeanCreationException {
        String methodPath = requestMethod + mapping;
        if (paths == null || paths.length == 0) {
            validateMapping(methodPath, method);
            addMapping(methodPath, instance, method);
            return;
        }

        for (String path : paths) {
            String combinedPath = methodPath + path;
            validateMapping(combinedPath, method);
            addMapping(combinedPath, instance, method);
        }
    }

    private void addMapping(String combinedPath, Object instance, Method method) {
        Matcher matcher = PATH_PARAMS_PATTERN.matcher(combinedPath);
        MethodHandler methodHandler = new MethodHandler(method, instance);
        if (!matcher.find()) {
            dispatcherServlet.mappings.put(combinedPath, methodHandler);
            return;
        }

        String pattern = matcher.replaceAll(REPLACEMENT_PATTERN_STR).replaceAll("\\?", "\\\\?");
        Pattern compile = Pattern.compile(pattern);
        dispatcherServlet.starMappings.put(compile, methodHandler);
    }

    private void validateMapping(String mapping, Method method) throws BeanCreationException {
        if (dispatcherServlet.mappings.containsKey(mapping)) {
            String message = String.format("Ambiguous mapping. Cannot map '%s' method.", method.getName());
            throw new BeanCreationException(message);
        }
    }

    private boolean isComponentInstance(Class<?> c) {
        return c.isAnnotationPresent(Component.class) || c.isAnnotationPresent(Service.class) ||
                c.isAnnotationPresent(Controller.class) || c.isAnnotationPresent(Configuration.class) ||
                c.isAnnotationPresent(RestController.class) || c.isAnnotationPresent(ControllerAdvice.class);
    }

    public ApplicationContext getContainer() {
        return applicationContext;
    }
}
