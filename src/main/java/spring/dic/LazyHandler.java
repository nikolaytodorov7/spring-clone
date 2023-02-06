package spring.dic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class LazyHandler implements InvocationHandler {
    Field field;
    Object instance;

    public LazyHandler(Field field, Object instance) {
        this.field = field;
        this.instance = instance;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Class<? extends Field> clazz = field.getClass();
        field.set(clazz, args);
        return method.invoke(clazz, args);
    }
}
