package tech.arhr.quingo.auth_service.exceptions.auth;

import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;

public class AuthException extends QuingoAppException {
    public AuthException(String message) {
        super(message);
    }
}
