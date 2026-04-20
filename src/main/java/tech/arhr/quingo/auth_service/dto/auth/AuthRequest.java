package tech.arhr.quingo.auth_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@NoArgsConstructor
public class AuthRequest {
    @NotNull
    @Size(min = 3, max = 100)
    @Email
    private String email;

    @NotNull
    @Size(min = 6, max = 50)
    private String password;
}
