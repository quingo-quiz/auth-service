package tech.arhr.quingo.auth_service.events.user;

import java.util.UUID;

public record UserPasswordResetEvent(UUID userId, String newPassword) {
}
