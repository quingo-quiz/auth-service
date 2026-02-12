package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import tech.arhr.quingo.auth_service.api.rest.errors.ErrorResponse;

public interface ErrorHandler<T> {
    ResponseEntity<ErrorResponse> handleError(T error, HttpServletRequest request);
}
