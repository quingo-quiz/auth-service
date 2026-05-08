package tech.arhr.quingo.auth_service.testutils;

import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TestUtils {

    private TestUtils() {}

    public static UserDto defaultUser() {
        return UserDto.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .roles(List.of(UserRole.USER))
                .build();
    }

    public static TokenDto token() {
        return TokenDto.builder()
                .id(UUID.randomUUID())
                .token("token-value")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
