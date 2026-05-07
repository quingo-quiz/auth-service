package tech.arhr.quingo.auth_service.api.rest.models;

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
public class UserApiModel {
    private UUID id;
    private String username;
    private String email;
    private String bio;
    private List<UserRole> roles;
    private boolean emailVerified;
    private AccountStatus accountStatus;
}
