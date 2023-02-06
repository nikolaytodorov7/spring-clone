package spring.server;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import spring.dic.ApplicationContext;

public class TomcatServer {
    private static final String DEFAULT_CONTEXT_PATH = "/";
    private static final String MAX_THREADS = "200";
    private static final int DEFAULT_PORT = 8080;

    private String contextPath = DEFAULT_CONTEXT_PATH; // todo check from config and override if present
    private int serverPort = DEFAULT_PORT; // todo check from config and override if present
    private String maxThreads = MAX_THREADS; // todo check from config and override if present
    private final ApplicationContext applicationContext;

    public TomcatServer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void startServer() throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(serverPort);
        tomcat.getConnector().setProperty("maxThreads", maxThreads);
        Context context = tomcat.addContext(contextPath, null);
        DispatcherServlet dispatcherServlet = applicationContext.getInstance(DispatcherServlet.class);
        String servletName = dispatcherServlet.getClass().getName();
        tomcat.addServlet(contextPath, servletName, dispatcherServlet);
        context.addServletMappingDecoded("/*", servletName);
        tomcat.start();
        tomcat.getServer().await();
    }
}
