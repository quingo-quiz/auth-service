package tech.arhr.quingo.auth_service.api.rest.models;

import org.springframework.http.HttpStatus;

import java.time.Instant;

public record SuccessResponse<T>(
        int status,
        String statusMessage,
        String message,
        T data,
        Instant timestamp
) {
    public static <T> SuccessResponse<T> of(HttpStatus status, T data, Instant timestamp) {
        return new SuccessResponse<>(status.value(), status.getReasonPhrase(), null, data, timestamp);
    }

    public static <T> SuccessResponse<T> of(HttpStatus status, String message, T data,  Instant timestamp) {
        return new SuccessResponse<>(status.value(), status.getReasonPhrase(), message, data, timestamp);
    }
}
