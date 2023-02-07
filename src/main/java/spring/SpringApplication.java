package spring;

import spring.config.ClassInjector;
import spring.config.MappersConfig;
import spring.exception.BeanCreationException;
import spring.server.TomcatServer;

import java.lang.reflect.InvocationTargetException;

public class SpringApplication {
    public static void run(Class<?> primaryClass, String[] args) {
        ClassInjector injector = new ClassInjector(primaryClass);
        MappersConfig config = new MappersConfig();
        config.configure(injector.getContext());
        config.createMappers(injector.getContext(), injector.classes);
        try {
            injector.registerClasses();
        } catch (BeanCreationException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        TomcatServer server = new TomcatServer(injector.getContext());
        try {
            server.startServer();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
