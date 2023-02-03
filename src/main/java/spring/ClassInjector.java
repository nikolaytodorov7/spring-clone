package spring;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.io.Resources;
import spring.annotation.*;
import spring.container.Container;
import spring.container.ContainerException;
import spring.exception.BeanCreationException;
import spring.server.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClassInjector {
    private static final String PROJECT_LOAD_FOLDER = "src\\main\\java\\"; //todo change folder to look for project when jar ...
    private static final Pattern STAR_PATTERN = Pattern.compile("\\{\\w+}");
    private static final String REPLACEMENT_PATTERN = "[\\\\w]+";

    private final Set<Class<?>> mapperClasses = new HashSet<>();
    private final MappersConfig config = new MappersConfig();
    private final Set<Class<?>> classes = new HashSet<>();
    private DispatcherServlet dispatcherServlet;
    private final Class<?> primaryClass;
    private Container container;

    public ClassInjector(Class<?> primaryClass) {
        this.primaryClass = primaryClass;
        createContainerWithProperties();
        try {
            dispatcherServlet = container.getInstance(DispatcherServlet.class); // DispatcherServlet will be created everytime
        } catch (Exception ignored) {
        }
    }

    private void createContainerWithProperties() {
        Properties properties;
        try {
            properties = Resources.getResourceAsProperties("application.properties");
        } catch (IOException e) {
            throw new RuntimeException("No properties file found!"); // todo check original ex
        }

        container = new Container(properties);
    }

    public void injectClasses() throws BeanCreationException {
        getClasses();
        registerInterfacesImplementation();
        config.configure(container, mapperClasses);
        registerClasses();
    }

    private void getClasses() {
        String classPath = primaryClass.getCanonicalName();
        String className = primaryClass.getSimpleName();
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

    private void registerInterfacesImplementation() throws ContainerException {
        for (Class<?> clazz : classes) {
            if (clazz.isInterface())
                continue;

            Arrays.stream(clazz.getInterfaces()).forEach((i) -> container.registerImplementation(i, clazz));
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
        Object classInstance = getComponentInstance(c);
        if (classInstance == null)
            return;

        RestController restController = c.getDeclaredAnnotation(RestController.class);
        RequestMapping requestMapping = c.getDeclaredAnnotation(RequestMapping.class);
        if (restController == null || requestMapping == null)
            return;

        String mapping = requestMapping.value() != null ? requestMapping.value()[0] : "";

        processFields(c, classInstance);
        processMethods(c, classInstance, mapping);
    }

    private void processFields(Class<?> c, Object instance) {
        Field[] fields = c.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            Autowired autowired = field.getDeclaredAnnotation(Autowired.class);
            if (autowired == null)
                continue;

            try {
                Object o = container.getInstance(field.getType());
                field.set(instance, o);
            } catch (Exception e) {
                throw new RuntimeException(e); //todo ex
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

    private void processMethodAnnotations(Object instance, String mapping, Method method, Annotation[] annotations) throws BeanCreationException {
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

    private void putMappings(String requestMethod, Object instance, String mapping, Method method, String[] paths) throws BeanCreationException {
        if (paths == null || paths.length == 0) {
            String combinedPath = requestMethod + mapping;
            validateMapping(combinedPath, method);
            addMapping(combinedPath, instance, method);
            return;
        }

        for (String path : paths) {
            String combinedPath = requestMethod + mapping + path;
            validateMapping(combinedPath, method);
            addMapping(combinedPath, instance, method);
        }
    }

    private void addMapping(String combinedPath, Object instance, Method method) {
        Matcher matcher = STAR_PATTERN.matcher(combinedPath);
        if (matcher.find()) {
            String pattern = STAR_PATTERN.matcher(combinedPath).replaceAll(REPLACEMENT_PATTERN);
            Pattern compile = Pattern.compile(pattern);
            dispatcherServlet.starMappings.put(compile, Map.entry(method, instance));
        } else
            dispatcherServlet.mappings.put(combinedPath, Map.entry(method, instance));
    }

    private void validateMapping(String mapping, Method method) throws BeanCreationException {
        if (dispatcherServlet.mappings.containsKey(mapping)) {
            String message = String.format("Ambiguous mapping. Cannot map '%s' method.", method.getName());
            throw new BeanCreationException(message);
        }
    }

    private Object getComponentInstance(Class<?> c) {
        if (isComponentInstance(c)) {
            try {
                return container.getInstance(c);
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private boolean isComponentInstance(Class<?> c) {
        return c.isAnnotationPresent(Component.class) || c.isAnnotationPresent(Service.class) ||
                c.isAnnotationPresent(Controller.class) || c.isAnnotationPresent(Configuration.class) ||
                c.isAnnotationPresent(RestController.class) || c.isAnnotationPresent(ControllerAdvice.class);
    }

    public Container getContainer() {
        return container;
    }
}
