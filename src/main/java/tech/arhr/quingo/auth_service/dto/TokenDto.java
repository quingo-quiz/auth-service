package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDto {
    private UUID id;
    private String token;
    private OffsetDateTime issuedAt;
    private OffsetDateTime expiresAt;
    private long secondsAlive;
    private boolean revoked;

    public static TokenEntity toEntity(TokenDto tokenDto) {
        return TokenEntity.builder()
                .id(tokenDto.getId())
                .token(tokenDto.getToken())
                .issuedAt(tokenDto.getIssuedAt())
                .expiresAt(tokenDto.getExpiresAt())
                .secondsAlive(tokenDto.getSecondsAlive())
                .revoked(tokenDto.isRevoked())
                .build();
    }

    public static TokenDto toDto(TokenEntity tokenEntity) {
        return TokenDto.builder()
                .id(tokenEntity.getId())
                .token(tokenEntity.getToken())
                .issuedAt(tokenEntity.getIssuedAt())
                .expiresAt(tokenEntity.getExpiresAt())
                .secondsAlive(tokenEntity.getSecondsAlive())
                .revoked(tokenEntity.isRevoked())
                .build();
    }
}
