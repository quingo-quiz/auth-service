package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.errors.ErrorResponse;

import java.time.OffsetDateTime;

@RestControllerAdvice
public class HttpMediaTypeNotSupportedHandler implements ErrorHandler<HttpMediaTypeNotSupportedException> {
    @Override
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleError(HttpMediaTypeNotSupportedException error, HttpServletRequest request) {
        HttpStatus status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;

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
}
