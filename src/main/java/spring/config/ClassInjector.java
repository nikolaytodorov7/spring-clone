package spring.config;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.io.Resources;
import spring.annotation.SpringBootApplication;
import spring.annotation.*;
import spring.dic.ApplicationContext;
import spring.dic.ApplicationContextException;
import spring.exception.BeanCreationException;
import spring.server.DispatcherServlet;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class ClassInjector {
    private final Set<Class<?>> componentInstanceAnnotations = Set.of(
            Component.class, Service.class, Controller.class, Configuration.class, RestController.class, ControllerAdvice.class);
    public final Set<Class<?>> classes = new HashSet<>();
    private final Set<Class<?>> excludeClasses = new HashSet<>();
    private final Class<?> primaryClass;
    private final DispatcherServlet dispatcherServlet;
    private ApplicationContext applicationContext;

    public ClassInjector(Class<?> primaryClass) {
        this.primaryClass = primaryClass;
        createContextWithProperties();
        dispatcherServlet = applicationContext.getInstance(DispatcherServlet.class);
    }

    private void createContextWithProperties() {
        Properties properties;
        try {
            properties = Resources.getResourceAsProperties("application.properties");
        } catch (IOException e) {
            throw new RuntimeException("No properties file found!");
        }

        applicationContext = new ApplicationContext(properties);
        try {
            injectClasses();
        } catch (BeanCreationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void injectClasses() throws BeanCreationException, InvocationTargetException, IllegalAccessException {
        SpringBootApplication springBootAppAnnotation = primaryClass.getDeclaredAnnotation(SpringBootApplication.class);
        Class<?>[] excludedClasses = null;
        if (springBootAppAnnotation != null) {
            excludedClasses = springBootAppAnnotation.exclude();
            excludeClasses.addAll(Arrays.asList(excludedClasses));
        }

        EnableAutoConfiguration autoConfigurationAnnotation = primaryClass.getDeclaredAnnotation(EnableAutoConfiguration.class);
        if (autoConfigurationAnnotation != null) {
            excludedClasses = autoConfigurationAnnotation.exclude();
            excludeClasses.addAll(Arrays.asList(excludedClasses));
        }

        if (excludedClasses == null)
            return;

        String packageName = getPackageName(primaryClass);
        File dir = new File(packageName);
        scanClasses(dir);

        registerInterfacesImplementation();
    }

    private String getPackageName(Class<?> c) {
        String classPath = c.getCanonicalName();
        String className = c.getSimpleName();
        int trimIndex = classPath.lastIndexOf('.' + className);
        return classPath.substring(0, trimIndex);
    }

    private void scanClasses(File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;

        for (File f : files) {
            loadClasses(f);
        }
    }

    private void loadClasses(File f) {
        if (f.isDirectory()) {
            scanClasses(f);
            return;
        }

        String classStr = f.toString();
        if (f.isFile() && classStr.endsWith(".class")) {
//            Class<?> c = createClass(classStr);

            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            String className = classStr
                    .substring(0, classStr.lastIndexOf(".class"))
                    .replace("\\", ".");

            Class<?> c;
            try {
                c = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                return;
            }


            if (c == null)
                return;

            if (excludeClasses.contains(c))
                return;

            ComponentScan componentScan = c.getDeclaredAnnotation(ComponentScan.class);
            if (componentScan != null) {
                scanComponents(c, componentScan);
                return;
            }

            if (c.isInterface())
                return;

            classes.add(c);
        }
    }

    private void scanComponents(Class<?> c, ComponentScan componentScan) {
        String[] value = componentScan.value();
        if (value == null || value.length == 0) {
            String packageName = getPackageName(c);
            scanClasses(new File(packageName));
            return;
        }

        for (String dir : value) {
            scanClasses(new File(dir));
        }
    }

    private void registerInterfacesImplementation() throws ApplicationContextException {
        for (Class<?> clazz : classes) {
            if (clazz.isInterface())
                continue;

            Arrays.stream(clazz.getInterfaces()).forEach((i) -> applicationContext.registerImplementation(i, clazz));
        }
    }

    public void registerClasses() throws BeanCreationException, InvocationTargetException, IllegalAccessException {
        for (Class<?> c : classes) {
            if (c.isInterface())
                continue;

            inject(c);
        }
    }

    private void inject(Class<?> c) throws BeanCreationException, InvocationTargetException, IllegalAccessException {
        Configuration configuration = c.getDeclaredAnnotation(Configuration.class);
        if (configuration != null) {
            extractBeansFromConfig(c);
            return;
        }

        if (!isComponent(c))
            return;

        Controller controller = c.getDeclaredAnnotation(Controller.class);
        RestController restController = c.getDeclaredAnnotation(RestController.class);
        if (restController == null && controller == null)
            return;

        Object classInstance = applicationContext.getInstance(c);
        dispatcherServlet.addController(c, classInstance, applicationContext);
    }

    private boolean isComponent(Class<?> c) {
        Annotation[] classAnnotations = c.getAnnotations();
        for (Annotation an : classAnnotations) {
            if (componentInstanceAnnotations.contains(an.annotationType()))
                return true;
        }

        return false;
    }

    private void extractBeansFromConfig(Class<?> c) throws InvocationTargetException, IllegalAccessException {
        Object instance = applicationContext.getInstance(c);
        Method[] methods = c.getDeclaredMethods();
        for (Method method : methods) {
            Bean beanAnnotation = method.getDeclaredAnnotation(Bean.class);
            if (beanAnnotation == null)
                continue;

            Object beanInstance = invokeBeanMethod(method, instance);
            Class<?> beanType = method.getReturnType();
            applicationContext.registerInstance(beanType, beanInstance);
        }
    }

    private Object invokeBeanMethod(Method method, Object instance) throws IllegalAccessException, InvocationTargetException {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0)
            return method.invoke(method, instance);

        Object[] parameters = new Object[parameterTypes.length];
        for (int i = 0; i < parameters.length; i++) {
            Object parameterInstance = applicationContext.getInstance(parameterTypes[i]);
            parameters[i] = parameterInstance;
        }

        return method.invoke(method, instance, parameters);
    }

    public ApplicationContext getContext() {
        return applicationContext;
    }
}
