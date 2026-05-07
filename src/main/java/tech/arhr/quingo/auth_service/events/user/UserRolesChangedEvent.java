package tech.arhr.quingo.auth_service.events.user;

import tech.arhr.quingo.auth_service.enums.UserRole;

import java.util.List;
import java.util.UUID;

public record UserRolesChangedEvent(UUID userId, List<UserRole> newRoles) {
}
