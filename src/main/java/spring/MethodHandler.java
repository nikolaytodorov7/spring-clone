package spring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

public class MethodHandler {
    public Method method;
    public Object instance;
    public Parameter[] parameters;

    public MethodHandler(Method method, Object instance) {
        this.method = method;
        this.instance = instance;
        parameters = method.getParameters();
    }

    public Object invoke(Object[] args) throws InvocationTargetException, IllegalAccessException {
        return method.invoke(instance, args);
    }
}
