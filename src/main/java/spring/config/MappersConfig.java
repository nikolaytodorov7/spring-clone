package spring.config;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import spring.dic.ApplicationContext;

import java.lang.reflect.Proxy;
import java.util.Set;

public class MappersConfig {
    private SqlSessionFactory sqlSessionFactory;
    private Configuration myBatisConfig;

    public void configure(ApplicationContext applicationContext) {
        String url = (String) applicationContext.getInstance("spring.datasource.url");
        String driver = (String) applicationContext.getInstance("spring.datasource.driverClassName");
        String username = (String) applicationContext.getInstance("spring.datasource.username");
        String password = (String) applicationContext.getInstance("spring.datasource.password");

        PooledDataSource dataSource = new PooledDataSource(driver, url, username, password); // PooledDataSource
        Environment environment = new Environment("environment", new JdbcTransactionFactory(), dataSource);
        myBatisConfig = new Configuration();
        myBatisConfig.setEnvironment(environment);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(myBatisConfig);
    }

    public void createMappers(ApplicationContext applicationContext, Set<Class<?>> mapperClasses) {
        for (Class<?> c : mapperClasses) {
            if (!c.isAnnotationPresent(Mapper.class) || !c.isInterface())
                continue;

            myBatisConfig.addMapper(c);
            Object mapper = Proxy.newProxyInstance(MappersConfig.class.getClassLoader(), new Class[]{c}, new MapperInvocationHandler(sqlSessionFactory, c));
            applicationContext.registerInstance(c, mapper);
        }
    }
}
