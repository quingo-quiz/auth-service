package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestValueException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.errors.ErrorResponse;

import java.time.OffsetDateTime;

@RestControllerAdvice
@Slf4j
public class MissingValueHandler implements ErrorHandler<MissingRequestValueException>{
    @Override
    @ExceptionHandler(MissingRequestValueException.class)
    public ResponseEntity<ErrorResponse> handleError(MissingRequestValueException error, HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

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
