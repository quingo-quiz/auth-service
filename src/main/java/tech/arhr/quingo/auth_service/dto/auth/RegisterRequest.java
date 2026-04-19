package tech.arhr.quingo.auth_service.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RegisterRequest {
    @NotNull
    @Size(min = 1, max = 50)
    private String username;

    @NotNull
    @Email
    @Size(min = 3, max = 100)
    private String email;

    @NotNull
    @Size(min = 6, max = 50)
    private String password;
}