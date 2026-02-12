package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private boolean isEmailVerified;
    private boolean isAccountBlocked;

    public static UserDto toDto(UserEntity entity){
        return UserDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .isEmailVerified(entity.isEmailVerified())
                .isAccountBlocked(entity.isAccountBlocked())
                .build();
    }

    public static UserEntity toEntity(UserDto dto){
        return UserEntity.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .isEmailVerified(dto.isEmailVerified())
                .isAccountBlocked(dto.isAccountBlocked())
                .build();
    }
}
