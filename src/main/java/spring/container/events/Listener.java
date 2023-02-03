package spring.container.events;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Listener {
    public Object instance;
    public Method method;
    public Class<?> type;

    public Listener(Object instance, Method method) {
        if (instance == null)
            throw new IllegalArgumentException("Instance can't be null!");

        if (method == null)
            throw new IllegalArgumentException("Method can't be null!");

        if (method.getParameterCount() != 1)
            throw new IllegalArgumentException("Method must have 1 parameter!");

        method.setAccessible(true);
        type = method.getParameterTypes()[0];
        this.method = method;
        this.instance = instance;
    }

    public Object invoke(Object event) {
        try {
            return method.invoke(instance, event);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Underlying method has thrown an exception!");
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Can't invoke inaccessible method!");
        }
    }
}