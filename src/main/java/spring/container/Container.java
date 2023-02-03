package spring.container;

import spring.container.annotation.Inject;
import spring.container.annotation.Named;
import spring.container.annotation.Default;
import spring.container.annotation.Lazy;
import spring.container.events.*;
import spring.container.events.EventListener;
import org.mockito.Mockito;

import java.lang.reflect.*;
import java.util.*;

public class Container {
    private final Map<String, Object> stringInstances = new HashMap<>();
    private final Map<Class<?>, Object> classInstances = new HashMap<>();
    private final Map<Class<?>, Class<?>> implementationInstances = new HashMap<>();
    private final Set<Class<?>> dependantClasses = new HashSet<>();
    private final ApplicationEventPublisher publisher = new ApplicationEventPublisher();

    public Container() {
    }

    public Container(Properties properties) {
        properties.forEach((k, v) -> stringInstances.put((String) k, v));
    }

    public Object getInstance(String key) throws ContainerException {
        Object instance = stringInstances.get(key);
        if (instance == null)
            throw new ContainerException(key + " has no registered instance.");

        return instance;
    }

    public <T> T getInstance(Class<T> c) throws Exception {
        Object instance = classInstances.get(c);
        if (instance != null)
            return (T) instance;

        Class<?> classImplementation = getImplementation(c);
        if (classImplementation == null)
            classImplementation = c;

        instance = classInstances.get(classImplementation);
        if (instance == null)
            instance = createInstance(c, classImplementation);

        classInstances.put(c, instance);
        setListeners(instance);
        return (T) instance;
    }

    public void decorateInstance(Object o) throws Exception {
        injectFields(o);
    }

    public void registerInstance(String key, Object instance) {
        if (!stringInstances.containsKey(key))
            stringInstances.put(key, instance);
    }

    public void registerImplementation(Class<?> c, Class<?> subClass) throws ContainerException {
        if (implementationInstances.containsKey(c))
            throw new ContainerException("Trying to register existing implementation");

        implementationInstances.put(c, subClass);
    }

    public void registerInstance(Class<?> c, Object instance) {
        if (!classInstances.containsKey(c))
            classInstances.put(c, instance);
    }

    public void registerInstance(Object instance) {
        registerInstance(instance.getClass(), instance);
    }

    private <T> Class<?> getImplementation(Class<T> c) throws ContainerException {
        if (!c.isInterface() || !Modifier.isAbstract(c.getModifiers()))
            return null;

        Class<?> classImplementation = implementationInstances.get(c);
        if (classImplementation != null)
            return classImplementation;

        Default defaultAnnotation = c.getDeclaredAnnotation(Default.class);
        if (defaultAnnotation != null) {
            Class<?> value = defaultAnnotation.value();
            if (value == null)
                throw new ContainerException("@Default annotation has no value");

            classImplementation = defaultAnnotation.value();
            implementationInstances.put(c, classImplementation);
        }

        if (classImplementation == null)
            throw new ContainerException("Interface provided without implementation!");

        return classImplementation;
    }

    private <T> Object createInstance(Class<T> c, Class<?> classImplementation) throws Exception {
        if (dependantClasses.contains(classImplementation))
            throw new ContainerException("Circular dependency class: " + classImplementation);

        dependantClasses.add(classImplementation);
        Object instance = initializeClass(c);
        dependantClasses.remove(classImplementation);
        return instance;
    }

    private void setListeners(Object instance) {
        Class<?> clazz = instance.getClass();
        if (instance instanceof ApplicationListener)
            extractListenerFromMethod(instance, clazz);
        else
            extractListenerFromAnnotation(instance, clazz);
    }

    private void extractListenerFromMethod(Object instance, Class<?> clazz) {
        Method method;
        try {
            method = clazz.getMethod("onApplicationEvent", ApplicationEvent.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Listener must have method 'onApplicationEvent' of type 'ApplicationEvent.class'!");
        }

        Listener listener = new Listener(instance, method);
        publisher.addListener(listener);
    }

    private void extractListenerFromAnnotation(Object instance, Class<?> clazz) {
        Method[] methods = clazz.getDeclaredMethods();
        for (Method method : methods) {
            EventListener eventListenerAnnotation = method.getDeclaredAnnotation(EventListener.class);
            if (eventListenerAnnotation == null)
                continue;

            Listener listener = new Listener(instance, method);
            publisher.addListener(listener);
        }
    }

    private Object initializeClass(Class<?> clazz) throws Exception {
        Object instance = createInstance(clazz);
        injectFields(instance);
        if (instance instanceof Initializer initializer)
            initializer.init();

        classInstances.put(clazz, instance);
        return instance;
    }

    private void injectFields(Object instance) throws Exception {
        Class<?> instanceClass = instance.getClass();
        Field[] fields = instanceClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            setField(instance, field);
        }
    }

    private void setField(Object instance, Field field) throws Exception {
        Inject injectAnnotation = field.getDeclaredAnnotation(Inject.class);
        if (injectAnnotation == null)
            return;

        Named namedAnnotation = field.getDeclaredAnnotation(Named.class);
        Class<?> fieldType = field.getType();
        Lazy lazyAnnotation = field.getDeclaredAnnotation(Lazy.class);
        if (lazyAnnotation != null) {
            Object mockedField = createLazyObject(instance, field, namedAnnotation, fieldType);
            field.set(instance, mockedField);
            return;
        }

        String fieldName = field.getName();
        if (namedAnnotation != null) {
            field.set(instance, getInstance(fieldName));
            return;
        }

        Object fieldInstance = dependantClasses.contains(fieldType) ?
                createLazyObject(instance, field, namedAnnotation, fieldType) : // for circular dependency
                getInstance(fieldType);
        field.set(instance, fieldInstance);
    }

    private Object createLazyObject(Object instance, Field field, Named namedAnnotation, Class<?> fieldType) {
        return Mockito.mock(fieldType, invocation -> {
            Object fieldInstance = namedAnnotation != null ?
                    getInstance(field.getName()) :
                    getInstance(field.getType());
            field.set(instance, fieldInstance);
            return invocation.getMethod().invoke(fieldInstance, invocation.getArguments());
        });
    }

    private Object createInstance(Class<?> clazz) throws Exception {
        Constructor<?> constructor = getInjectedConstructor(clazz);
        Object[] params = getConstructorParameters(constructor);
        return constructor.newInstance(params);
    }

    private Constructor<?> getInjectedConstructor(Class<?> clazz) throws ContainerException, NoSuchMethodException {
        Constructor<?> constructor = null;
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        for (Constructor<?> ctor : constructors) {
            ctor.setAccessible(true);
            Inject injectAnnotation = ctor.getDeclaredAnnotation(Inject.class);
            if (injectAnnotation == null)
                continue;

            if (constructor != null)
                throw new ContainerException("Found more than one constructor with @Inject annotation!");

            constructor = ctor;
        }

        return constructor != null ? constructor : clazz.getDeclaredConstructor();
    }

    private Object[] getConstructorParameters(Constructor<?> constructor) throws Exception {
        Parameter[] constructorParameters = constructor.getParameters();
        if (constructorParameters.length == 0)
            return null;

        Object[] parameters = new Object[constructorParameters.length];
        for (int i = 0; i < constructorParameters.length; i++) {
            Parameter parameter = constructorParameters[i];
            Named namedAnnotation = parameter.getDeclaredAnnotation(Named.class);
            if (namedAnnotation != null) {
                String namedValue = namedAnnotation.value();
                parameters[i] = getInstance(namedValue);
                continue;
            }

            Class<?> parameterType = parameter.getType();
            parameters[i] = getInstance(parameterType);
        }

        return parameters;
    }
}
