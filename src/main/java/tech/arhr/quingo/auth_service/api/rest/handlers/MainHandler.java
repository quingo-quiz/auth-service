package tech.arhr.quingo.auth_service.api.rest.handlers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;

import java.time.OffsetDateTime;


@RestControllerAdvice
public class MainHandler {
    @ExceptionHandler(QuingoAppException.class)
    public ResponseEntity<AppError> handleException(QuingoAppException ex) {
        AppError serviceError = AppError.builder()
                .status(400)
                .errorMessage(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(serviceError);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class AppError {
        private OffsetDateTime timestamp;
        private int status;
        private String errorMessage;
    }
}
