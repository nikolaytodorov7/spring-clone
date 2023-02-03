//package springrestapi.interceptor;
//
//import spring.annotation.Component;
//import springrestapi.exception.JsonResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//@Component
//public class LoggerInterceptor implements HandlerInterceptor {
//    public JsonResponse jsonResponse = null;
//    private static final Logger LOGGER = LoggerFactory.getLogger(LoggerInterceptor.class);
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/MM/uuuu");
//        LocalDateTime now = LocalDateTime.now();
//        String method = request.getMethod();
//        String requestURI = request.getRequestURI();
//
//        String msg = String.format("[%s] %s %s", dtf.format(now), method, requestURI);
//        LOGGER.info(msg);
//        return true;
//    }
//
//    @Override
//    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
//        if (jsonResponse != null) {
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/MM/uuuu");
//            LocalDateTime now = LocalDateTime.now();
//            int status = jsonResponse.status;
//            String error = jsonResponse.error;
//            String message = jsonResponse.message;
//            String msg = String.format("[%s] %s %s: %s", dtf.format(now), status, error, message);
//            LOGGER.error(msg);
//            return;
//        }
//
//        if (ex != null) {
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss MM/dd/MM/uuuu");
//            LocalDateTime now = LocalDateTime.now();
//            int status = response.getStatus();
//            String message = ex.getMessage();
//            String msg = String.format("[%s] %s: %s", dtf.format(now), status, message);
//            LOGGER.error(msg);
//        }
//    }
//}
