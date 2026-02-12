package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.errors.ErrorResponse;
import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;

import java.time.OffsetDateTime;


@RestControllerAdvice
public class QuingoAppExceptionHandler implements ErrorHandler<QuingoAppException> {
    @Override
    @ExceptionHandler(QuingoAppException.class)
    public ResponseEntity<ErrorResponse> handleError(QuingoAppException error,  HttpServletRequest request) {
        HttpStatus status = findResponseStatus(error.getClass());

        ErrorResponse serviceError = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorMessage(error.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status).body(serviceError);
    }

    private HttpStatus findResponseStatus(Class<?> clazz) {
        while (clazz != null && clazz != Object.class) {
            ResponseStatus status = clazz.getAnnotation(ResponseStatus.class);
            if (status != null) {
                return status.value();
            }
            clazz = clazz.getSuperclass();
        }
        return HttpStatus.BAD_REQUEST;
    }
}
