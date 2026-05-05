package tech.arhr.quingo.auth_service.exceptions.auth;

public class EmailAlreadyVerifiedException extends AuthException {
    public EmailAlreadyVerifiedException(String message) {
        super(message);
    }
    public EmailAlreadyVerifiedException() {
        super("Email already verified");
    }
}
