package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SendResetPasswordRequest {
    @NotNull
    @Size(min = 3, max = 100)
    @Email
    private String email;
}
