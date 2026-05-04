package tech.arhr.quingo.auth_service.dto;

import lombok.Data;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;

import java.util.UUID;

@Data
public class VerificationTokenDto {
    private String token;
    private UUID userId;
    private VerificationTokenType type;
}
