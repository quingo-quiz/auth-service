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
    private boolean revoked;
    private UserDto userDto;

    public static TokenEntity toEntity(TokenDto tokenDto) {
        TokenEntity entity = TokenEntity.builder()
                .id(tokenDto.getId())
                .token(tokenDto.getToken())
                .issuedAt(tokenDto.getIssuedAt())
                .expiresAt(tokenDto.getExpiresAt())
                .revoked(tokenDto.isRevoked())
                .build();
        if (tokenDto.getUserDto() != null) {
            entity.setUser(UserDto.toEntity(tokenDto.getUserDto()));
        }
        return entity;
    }

    public static TokenDto toDto(TokenEntity tokenEntity) {
        TokenDto dto = TokenDto.builder()
                .id(tokenEntity.getId())
                .token(tokenEntity.getToken())
                .issuedAt(tokenEntity.getIssuedAt())
                .expiresAt(tokenEntity.getExpiresAt())
                .revoked(tokenEntity.isRevoked())
                .build();
        if (tokenEntity.getUser() != null) {
            dto.setUserDto(UserDto.toDto(tokenEntity.getUser()));
        }
        return dto;
    }
}
