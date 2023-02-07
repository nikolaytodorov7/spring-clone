package spring;

import spring.config.ClassInjector;
import spring.exception.BeanCreationException;
import spring.server.TomcatServer;

public class SpringApplication {
    public static void run(Class<?> primaryClass, String[] args) { // todo use args
        ClassInjector injector = new ClassInjector(primaryClass);
        try {
            injector.injectClasses();
        } catch (BeanCreationException e) {
            e.printStackTrace();
            return;
        }

        TomcatServer server = new TomcatServer(injector.getContainer());
        try {
            server.startServer();
        } catch (Exception e) {
            throw new RuntimeException(e); // todo ?ex
        }
    }
}
