package tech.arhr.quingo.auth_service.events;

import java.util.UUID;

public record AllUserSessionsInvalidatedEvent(UUID userId) {
}
