package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangePasswordRequest {
    @NotNull
    @Length(min = 6, max = 50)
    private String oldPassword;

    @NotNull
    @Length(min = 6, max = 50)
    private String newPassword;
}
