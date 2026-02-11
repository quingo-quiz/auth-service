package tech.arhr.quingo.auth_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import tech.arhr.quingo.auth_service.providers.AuthProviderType;

@Data
@NoArgsConstructor
public class RegisterRequest {
    private String provider = "LOCAL";

    @Size(min = 1, max = 50)
    private String username;

    @Email
    @Size(min = 3, max = 100)
    private String email;

    @Size(min = 6, max = 50)
    private String password;
}