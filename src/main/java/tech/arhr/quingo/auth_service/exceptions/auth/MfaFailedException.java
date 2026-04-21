package tech.arhr.quingo.auth_service.exceptions.auth;

public class MfaFailedException extends AuthException {
    public MfaFailedException(String message) {
        super(message);
    }
}
