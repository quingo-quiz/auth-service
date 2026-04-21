package tech.arhr.quingo.auth_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
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
    private boolean emailVerified;
    private boolean mfaEnabled;
    private AccountStatus accountStatus;
}
