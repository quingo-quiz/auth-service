package tech.arhr.quingo.auth_service.exceptions.auth;

public class PasswordNotSetException extends AuthException {
    public PasswordNotSetException(String message) {
        super(message);
    }

    public PasswordNotSetException() {
        super("Your account password not set. Please use another method.");
    }
}
