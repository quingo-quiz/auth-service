package tech.arhr.quingo.auth_service.dto.auth;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OtpVerifyRequest {
    @NotNull
    @Size(min = 1, max = 32)
    private String code;

    @Size(max = 500)
    @NotNull
    private String mfaTempToken;
}
