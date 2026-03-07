package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.models.ErrorResponse;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.time.OffsetDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
public class HttpMediaTypeNotSupportedHandler implements ErrorHandler<HttpMediaTypeNotSupportedException> {
    private final TimeProvider timeProvider;

    @Override
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleError(HttpMediaTypeNotSupportedException error, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;

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
}
