package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    private UUID id;
    private String token;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean revoked;
    private UserDto userDto;
}
