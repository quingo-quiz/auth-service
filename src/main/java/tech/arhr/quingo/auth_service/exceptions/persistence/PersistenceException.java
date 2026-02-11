package tech.arhr.quingo.auth_service.exceptions.persistence;

import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;

public class PersistenceException extends QuingoAppException {
    public PersistenceException(String message) {
        super(message);
    }
}
