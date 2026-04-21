package tech.arhr.quingo.auth_service.exceptions.auth;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MfaSettingsInvalidException extends AuthException {
    public MfaSettingsInvalidException(String message) {
        super(message);
    }
}
