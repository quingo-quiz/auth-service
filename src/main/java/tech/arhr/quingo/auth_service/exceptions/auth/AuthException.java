package tech.arhr.quingo.auth_service.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class AuthException extends QuingoAppException {
    public AuthException(String message) {
        super(message);
    }
}
