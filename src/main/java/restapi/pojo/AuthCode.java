package restapi.pojo;

public enum AuthCode {
    UNAUTH(0),
    FORBIDDEN(1),
    OK(2);

    public final int value;

    AuthCode(int value) {
        this.value = value;
    }
}
