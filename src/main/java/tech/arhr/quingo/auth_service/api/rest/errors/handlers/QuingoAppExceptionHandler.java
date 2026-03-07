package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.models.ErrorResponse;
import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.time.OffsetDateTime;


@RestControllerAdvice
@RequiredArgsConstructor
public class QuingoAppExceptionHandler implements ErrorHandler<QuingoAppException> {
    private final TimeProvider timeProvider;

    @Override
    @ExceptionHandler(QuingoAppException.class)
    public ResponseEntity<ErrorResponse> handleError(QuingoAppException error,  HttpServletRequest request) {
        HttpStatus status = findResponseStatus(error.getClass());

        ErrorResponse serviceError = ErrorResponse.builder()
                .status(status.value())
                .statusMessage(status.getReasonPhrase())
                .message(error.getMessage())
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(timeProvider.now())
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
