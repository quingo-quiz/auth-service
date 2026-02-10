package tech.arhr.quingo.auth_service.api.rest.handlers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tech.arhr.quingo.auth_service.exceptions.ServiceException;

import java.time.OffsetDateTime;


@RestControllerAdvice
public class ServiceHandler {
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ServiceError> handleException(ServiceException ex) {
        ServiceError serviceError = ServiceError.builder()
                .status(400)
                .error(ex.getMessage())
                .timestamp(OffsetDateTime.now())
                .build();

        return ResponseEntity.badRequest().body(serviceError);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ServiceError{
        private OffsetDateTime timestamp;
        private int status;
        private String error;
    }
}
