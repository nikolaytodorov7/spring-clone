package spring.config;

import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import spring.dic.ApplicationContext;

import java.util.Set;

public class MappersConfig {
    private static final String SPRING_DATASOURCE_DRIVER_CLASSNAME_PROPERTY_KEY = "spring.datasource.driverClassName";
    private static final String SPRING_DATASOURCE_URL_PROPERTY_KEY = "spring.datasource.url";
    private static final String SPRING_DATASOURCE_USERNAME_PROPERTY_KEY = "spring.datasource.username";
    private static final String SPRING_DATASOURCE_PASSWORD_PROPERTY_KEY = "spring.datasource.password";
    private SqlSessionFactory sqlSessionFactory;

    public void configure(ApplicationContext applicationContext, Set<Class<?>> mapperClasses) {
        String url = (String) applicationContext.getInstance(SPRING_DATASOURCE_URL_PROPERTY_KEY);
        String driver = (String) applicationContext.getInstance(SPRING_DATASOURCE_DRIVER_CLASSNAME_PROPERTY_KEY);
        String username = (String) applicationContext.getInstance(SPRING_DATASOURCE_USERNAME_PROPERTY_KEY);
        String password = (String) applicationContext.getInstance(SPRING_DATASOURCE_PASSWORD_PROPERTY_KEY);

        UnpooledDataSource dataSource = new UnpooledDataSource(driver, url, username, password);
        Environment environment = new Environment("environment", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = createConfiguration(environment, mapperClasses);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        createMappers(applicationContext, mapperClasses);
    }

    private Configuration createConfiguration(Environment environment, Set<Class<?>> mapperClasses) {
        Configuration configuration = new Configuration();
        configuration.setEnvironment(environment);
        for (Class<?> c : mapperClasses) {
            configuration.addMapper(c);
            mapperClasses.add(c);
        }

        return configuration;
    }

    private void createMappers(ApplicationContext applicationContext, Set<Class<?>> mapperClasses) {
        for (Class<?> c : mapperClasses) {
            SqlSession session = sqlSessionFactory.openSession();
            Object mapper = session.getMapper(c);
            applicationContext.registerInstance(c, mapper);
        }
    }
}
