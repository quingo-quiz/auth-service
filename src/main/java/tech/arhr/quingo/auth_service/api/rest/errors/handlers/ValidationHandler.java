package tech.arhr.quingo.auth_service.api.rest.errors.handlers;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.api.rest.errors.ErrorResponse;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidationHandler implements ErrorHandler<MethodArgumentNotValidException> {

    @Override
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleError(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        Map<String, Object> rejectedValues = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                String fieldName = fieldError.getField();
                String message = fieldError.getDefaultMessage();
                Object rejectedValue = fieldError.getRejectedValue();

                fieldErrors.put(fieldName, message);
                rejectedValues.put(fieldName, rejectedValue);
            }
        });

        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorMessage("Validation Failed")
                .path(request.getRequestURI())
                .method(request.getMethod())
                .fieldErrors(fieldErrors)
                .rejectedValues(rejectedValues)
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.status(status).body(error);
    }
}
