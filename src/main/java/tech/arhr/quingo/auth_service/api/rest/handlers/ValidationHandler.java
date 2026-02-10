package tech.arhr.quingo.auth_service.api.rest.handlers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ValidationHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationError> handleException(MethodArgumentNotValidException ex) {
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

        ValidationError error = ValidationError.builder()
                .timestamp(OffsetDateTime.now())
                .status(400)
                .error("Validation Failed")
                .fieldErrors(fieldErrors)
                .rejectedValues(rejectedValues)
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ValidationError{
        private OffsetDateTime timestamp;
        private int status;
        private String error;
        private Map<String, String> fieldErrors;
        private Map<String, Object> rejectedValues;
    }
}
