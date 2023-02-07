package spring.config;

import spring.exception.ResponseException;

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
        Object invoked;
        try {
            invoked = method.invoke(instance, args);
        } catch (Exception e) {
            throw new ResponseException(400, "Bad Req msg test"); // todo exceptions handling
        }

        return invoked;
    }
}
