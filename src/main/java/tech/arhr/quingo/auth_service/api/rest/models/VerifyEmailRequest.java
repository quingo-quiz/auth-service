package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @NotNull
    private String verificationToken;
}
