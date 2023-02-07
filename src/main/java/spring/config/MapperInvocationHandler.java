package spring.config;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class MapperInvocationHandler implements InvocationHandler {
    private final SqlSessionFactory sqlSessionFactory;
    private final Class<?> mapperClass;

    public MapperInvocationHandler(SqlSessionFactory sqlSessionFactory, Class<?> mapperClass) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.mapperClass = mapperClass;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            Object mapper = session.getMapper(mapperClass);
            return method.invoke(mapper, args);
        }
    }
}