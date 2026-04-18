package tech.arhr.quingo.auth_service.api.rest.models;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import tech.arhr.quingo.auth_service.enums.UserRole;

import java.util.List;

@Data
public class ChangeUserRolesRequest {
    @NotNull
    @NotEmpty
    private List<UserRole> userRoles;
}
