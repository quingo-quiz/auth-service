package tech.arhr.quingo.auth_service.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    @Mock
    private TimeProvider timeProvider;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(timeProvider);
        ReflectionTestUtils.setField(jwtProvider, "JWT_SECRET", "secretkey1234567890");
        ReflectionTestUtils.setField(jwtProvider, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(jwtProvider, "ACCESS_EXPIRATION_MINUTES", 60);
        ReflectionTestUtils.setField(jwtProvider, "REFRESH_EXPIRATION_DAYS", 7);
        when(timeProvider.now()).thenReturn(Instant.now());
        jwtProvider.init();
    }

    @Test
    void createAndValidateAccessToken_Success() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .username("u")
                .email("e@example.com")
                .emailVerified(true)
                .roles(List.of(UserRole.USER))
                .build();

        var tokenDto = jwtProvider.createAccessToken(user, UUID.randomUUID());

        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.getToken()).isNotEmpty();

        var validatedId = jwtProvider.validateAccessToken(tokenDto.getToken());
        assertThat(validatedId).isEqualTo(tokenDto.getId());
    }

    @Test
    void validateRefreshToken_WithAccessToken_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .username("u")
                .email("e@example.com")
                .emailVerified(true)
                .roles(List.of(UserRole.USER))
                .build();

        var access = jwtProvider.createAccessToken(user, UUID.randomUUID());

        assertThatThrownBy(() -> jwtProvider.validateRefreshToken(access.getToken()));
    }
}
