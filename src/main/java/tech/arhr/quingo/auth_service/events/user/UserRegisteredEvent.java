package tech.arhr.quingo.auth_service.events.user;

import tech.arhr.quingo.auth_service.dto.UserDto;

public record UserRegisteredEvent(UserDto user) {
}
