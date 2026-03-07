package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.utils.UserMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
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
