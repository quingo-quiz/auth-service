package tech.arhr.quingo.auth_service.api.rest.errors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String errorMessage;
    private String path;
    private String method;
    private OffsetDateTime timestamp;

    private Map<String, String> fieldErrors;
    private Map<String, Object> rejectedValues;

    private Map<String, Object> details;
}
