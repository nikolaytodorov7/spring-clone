package spring.dic;

public class ApplicationContextException extends RuntimeException {
    public ApplicationContextException() {
        super();
    }

    public ApplicationContextException(String message) {
        super(message);
    }

    public ApplicationContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public ApplicationContextException(Throwable cause) {
        super(cause);
    }
}
