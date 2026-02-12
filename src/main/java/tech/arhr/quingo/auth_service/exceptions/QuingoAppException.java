package tech.arhr.quingo.auth_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class QuingoAppException extends RuntimeException {
    public QuingoAppException(String message) {
        super(message);
    }
}
