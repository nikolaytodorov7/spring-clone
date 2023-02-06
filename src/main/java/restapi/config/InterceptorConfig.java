//package springrestapi.config;
//
//import spring.annotation.Autowired;
//import spring.annotation.Configuration;
//import springrestapi.interceptor.AuthInterceptor;
//import springrestapi.interceptor.LoggerInterceptor;
//
//@Configuration
//public class InterceptorConfig implements WebMvcConfigurer {
//    @Autowired
//    AuthInterceptor authInterceptor;
//    @Autowired
//    LoggerInterceptor loggerInterceptor;
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        registry.addInterceptor(authInterceptor);
//        registry.addInterceptor(loggerInterceptor);
//    }
//}
