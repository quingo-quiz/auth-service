package tech.arhr.quingo.auth_service.exceptions.auth;

public class EmailAlreadyExistsException extends AuthException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
    public EmailAlreadyExistsException(){
        super("Email already exists");
    }
}
