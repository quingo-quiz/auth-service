package tech.arhr.quingo.auth_service.events.user;

import java.util.UUID;

public record UserEmailVerifiedEvent(UUID userId) {
}
