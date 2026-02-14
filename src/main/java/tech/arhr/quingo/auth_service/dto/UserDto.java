package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.enums.UserRole;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserDto {
    private UUID id;
    private String username;
    private String email;
    private List<UserRole> roles;
    private boolean isEmailVerified;
    private boolean isAccountBlocked;

    public static UserDto toDto(UserEntity entity){
        return UserDto.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .email(entity.getEmail())
                .roles(entity.getRoles())
                .isEmailVerified(entity.isEmailVerified())
                .isAccountBlocked(entity.isAccountBlocked())
                .build();
    }

    public static UserEntity toEntity(UserDto dto){
        return UserEntity.builder()
                .id(dto.getId())
                .username(dto.getUsername())
                .email(dto.getEmail())
                .roles(dto.getRoles())
                .isEmailVerified(dto.isEmailVerified())
                .isAccountBlocked(dto.isAccountBlocked())
                .build();
    }
}
