package tech.arhr.quingo.auth_service.exceptions.auth;

public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
