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

    private static final String TEST_PRIVATE_KEY = "-----BEGIN PRIVATE KEY----- MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgJdcksMsCpIFzeHpFPxIGa7SOpAvFRXgCj72QBc5EOQWhRANCAASNBDZrkVsQu9Sr5mM72tt1vO4jhjG1a5y1NvNmtjbnGncZia9hcd0mbEpZKfST6pteOw3bK0lvTkNIoPpsga7f -----END PRIVATE KEY-----";
    private static final String TEST_PUBLIC_KEY = "-----BEGIN PUBLIC KEY----- MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjQQ2a5FbELvUq+ZjO9rbdbzuI4YxtWuctTbzZrY25xp3GYmvYXHdJmxKWSn0k+qbXjsN2ytJb05DSKD6bIGu3w== -----END PUBLIC KEY-----";

    @Mock
    private TimeProvider timeProvider;

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(timeProvider);
        ReflectionTestUtils.setField(jwtProvider, "privateKeyPem", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(jwtProvider, "publicKeyPem", TEST_PUBLIC_KEY);
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
