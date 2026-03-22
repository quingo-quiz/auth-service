package tech.arhr.quingo.auth_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class VerificationTokenDto {
    private String token;
    private UUID userId;
}
