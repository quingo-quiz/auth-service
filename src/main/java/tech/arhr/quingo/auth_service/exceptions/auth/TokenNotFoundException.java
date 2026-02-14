package tech.arhr.quingo.auth_service.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class TokenNotFoundException extends AuthException {
    public TokenNotFoundException(String message) {
        super(message);
    }
    public TokenNotFoundException() {
        super("JWT token not found");
    }
}
