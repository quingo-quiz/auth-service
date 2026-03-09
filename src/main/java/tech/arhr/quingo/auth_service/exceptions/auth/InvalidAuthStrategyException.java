package tech.arhr.quingo.auth_service.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAuthStrategyException extends AuthException {
    public InvalidAuthStrategyException() {
        super("Invalid Auth Strategy");
    }
    public InvalidAuthStrategyException(String strategy) {
        super("Invalid Auth Strategy: " + strategy);
    }
}
