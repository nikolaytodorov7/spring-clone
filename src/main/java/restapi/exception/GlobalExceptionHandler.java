//package restapi.exception;
//
//import spring.annotation.ControllerAdvice;
//import spring.annotation.ExceptionHandler;
//import spring.annotation.RestController;
//
//@ControllerAdvice
//@RestController
//public class GlobalExceptionHandler {
//    @Autowired
//    LoggerInterceptor loggerInterceptor;
//
//    private final int BAD_REQUEST_STATUS_CODE = 400;
//    private final int UNAUTHORIZED_STATUS_CODE = 401;
//    private final int NOT_FOUND_STATUS_CODE = 404;
//    private final String BAD_REQUEST_ERROR_MESSAGE = "Bad Request";
//    private final String UNAUTHORIZED_ERROR_MESSAGE = "Unauthorized";
//    private final String NOT_FOUND_ERROR_MESSAGE = "Not Found";
//
//    @ExceptionHandler(value = NotFoundException.class)
//    public ResponseEntity<JsonResponse> handleNotFound(Exception e) {
//        loggerInterceptor.jsonResponse = new JsonResponse(NOT_FOUND_STATUS_CODE, NOT_FOUND_ERROR_MESSAGE, e.getMessage());
//        return new ResponseEntity<>(new JsonResponse(NOT_FOUND_STATUS_CODE, NOT_FOUND_ERROR_MESSAGE, e.getMessage()), HttpStatus.NOT_FOUND);
//    }
//
//    @ExceptionHandler(value = IllegalDataException.class)
//    public ResponseEntity<JsonResponse> handleIllegalData(Exception e) {
//        return new ResponseEntity<>(new JsonResponse(BAD_REQUEST_STATUS_CODE, BAD_REQUEST_ERROR_MESSAGE, e.getMessage()), HttpStatus.BAD_REQUEST);
//    }
//}