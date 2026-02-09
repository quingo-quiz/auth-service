package tech.arhr.quingo.auth_service.api.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@NoArgsConstructor
public class RegisterRequest {

    @Range(min = 1, max = 50)
    @NotNull
    private String username;

    @Email
    @Range(min = 3, max = 100)
    @NotNull
    private String email;

    @Range(min = 6, max = 50)
    @NotNull
    private String password;
}
