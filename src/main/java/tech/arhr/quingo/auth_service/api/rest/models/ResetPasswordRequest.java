package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotNull
    private String resetToken;

    @NotNull
    @Size(min = 6, max = 50)
    private String newPassword;
}
