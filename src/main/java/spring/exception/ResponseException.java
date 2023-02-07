package spring.exception;

public class ResponseException extends RuntimeException {
    public int statusCode;
    public String message;

    public ResponseException(int statusCode, String message) {
        this.statusCode = statusCode;
        this.message = message;
    }
}
