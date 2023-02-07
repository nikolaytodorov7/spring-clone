package spring.server;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.descriptor.web.FilterDef;
import org.apache.tomcat.util.descriptor.web.FilterMap;
import spring.dic.ApplicationContext;
import spring.filter.ExceptionFilter;

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
        addDispatcherServlet(tomcat, context);
        addExceptionFilter(context);

        tomcat.start();
        tomcat.getServer().await();
    }

    private static void addExceptionFilter(Context context) {
        ExceptionFilter exceptionFilter = new ExceptionFilter();
        FilterDef filterDef = new FilterDef();
        filterDef.setFilter(exceptionFilter);
        filterDef.setFilterName(exceptionFilter.getClass().getName());
        FilterMap filterMap = new FilterMap();
        filterMap.setFilterName(exceptionFilter.getClass().getName());
        filterMap.addURLPattern("/*");
        context.addFilterDef(filterDef);
        context.addFilterMap(filterMap);
    }

    private void addDispatcherServlet(Tomcat tomcat, Context context) {
        DispatcherServlet dispatcherServlet = applicationContext.getInstance(DispatcherServlet.class);
        String servletName = dispatcherServlet.getClass().getName();
        tomcat.addServlet(contextPath, servletName, dispatcherServlet);
        context.addServletMappingDecoded("/*", servletName);
    }
}
