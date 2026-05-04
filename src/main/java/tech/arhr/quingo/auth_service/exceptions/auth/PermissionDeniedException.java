package tech.arhr.quingo.auth_service.exceptions.auth;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class PermissionDeniedException extends AuthException {
    public PermissionDeniedException(String message) {
        super(message);
    }
    public PermissionDeniedException() {
        super("Permission denied for this action");
    }
}
