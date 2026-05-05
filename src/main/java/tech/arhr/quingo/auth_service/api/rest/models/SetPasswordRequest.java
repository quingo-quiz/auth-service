package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class SetPasswordRequest {
    @NotNull
    @Length(min = 6, max = 50)
    private String password;
}
