package tech.arhr.quingo.auth_service.exceptions.auth;

public class InvalidTokenException extends AuthException {
    public InvalidTokenException(String message) {
        super(message);
    }
    public InvalidTokenException() {
        super("Invalid token");
    }
}
