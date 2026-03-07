package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.models.ErrorResponse;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.time.OffsetDateTime;

@RestControllerAdvice
@RequiredArgsConstructor
public class MissingValueHandler implements ErrorHandler<MissingRequestValueException>{
    private final TimeProvider timeProvider;

    @Override
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponse> handleError(MissingRequestValueException error, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

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
