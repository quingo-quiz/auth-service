package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.JwtProvider;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final String TEST_PRIVATE_KEY = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgJdcksMsCpIFzeHpFPxIGa7SOpAvFRXgCj72QBc5EOQWhRANCAASNBDZrkVsQu9Sr5mM72tt1vO4jhjG1a5y1NvNmtjbnGncZia9hcd0mbEpZKfST6pteOw3bK0lvTkNIoPpsga7f";
    private static final String TEST_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEjQQ2a5FbELvUq+ZjO9rbdbzuI4YxtWuctTbzZrY25xp3GYmvYXHdJmxKWSn0k+qbXjsN2ytJb05DSKD6bIGu3w==";
    private static final String OTHER_PRIVATE_KEY = "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg6Q/nuzqERRks5YlahTYUAK1qa3x7fulzeWmcEosQ0+KhRANCAASR8AOMy285a5L2AyGFTbgU0b9nzG5FxQybNh0DQEfTfz6unLoMS0QzvvmDQ4nOpsfB8FP7NGg5IYg3wppCAMbK";
    private static final String OTHER_PUBLIC_KEY = "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEkfADjMtvOWuS9gMhhU24FNG/Z8xuRcUMmzYdA0BH038+rpy6DEtEM775g0OJzqbHwfBT+zRoOSGIN8KaQgDGyg==";

    @Mock
    private JpaTokenRepository jpaTokenRepository;

    @Mock
    private Hasher hasher;

    @Mock
    private TokenMapper tokenMapper;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private AccessTokensRevocationService revocationService;

    private JwtProvider jwtProvider;

    private SessionService sessionService;

    private UserDto defaultUser;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(timeProvider);
        ReflectionTestUtils.setField(jwtProvider, "privateKeyPem", TEST_PRIVATE_KEY);
        ReflectionTestUtils.setField(jwtProvider, "publicKeyPem", TEST_PUBLIC_KEY);
        ReflectionTestUtils.setField(jwtProvider, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(jwtProvider, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(jwtProvider, "REFRESH_EXPIRATION_DAYS", 7);
        jwtProvider.init();

        sessionService = new SessionService(jpaTokenRepository, revocationService, hasher, tokenMapper, timeProvider, jwtProvider);

        defaultUser = UserDto.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .roles(List.of(UserRole.USER))
                .build();

        lenient().when(timeProvider.now()).thenReturn(Instant.now());
        lenient().when(tokenMapper.toEntity(any(TokenDto.class))).thenAnswer(invocation -> new TokenEntity());
        lenient().when(hasher.hash(any())).thenReturn("hashed_token");
    }

    @Test
    void createAccessToken_NullRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(null)
                .build();

        assertThatThrownBy(() -> sessionService.createSession(user, null))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_EmptyRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(List.of())
                .build();

        assertThatThrownBy(() -> sessionService.createSession(user, null))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_ValidUser_TokenContainsCorrectClaims() {
        TokenDto result = sessionService.createSession(defaultUser, null).getAccessToken();
        DecodedJWT decoded = jwtProvider.decodeToken(result.getToken());

        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("access");
        assertThat(decoded.getClaim("username").asString()).isEqualTo(defaultUser.getUsername());
        assertThat(decoded.getClaim("email").asString()).isEqualTo(defaultUser.getEmail());
        assertThat(result.getExpiresAt()).isAfter(result.getIssuedAt());
    }

    @Test
    void createRefreshToken_ValidUser_SavesHashedTokenAndReturnsDto() {
        TokenEntity entity = new TokenEntity();
        when(tokenMapper.toEntity(any(TokenDto.class))).thenReturn(entity);
        when(hasher.hash(any())).thenReturn("hashed_token");
        TokenDto result = sessionService.createSession(defaultUser, null).getRefreshToken();
        DecodedJWT decoded = jwtProvider.decodeToken(result.getToken());

        verify(jpaTokenRepository).save(entity);
        assertThat(entity.getToken()).isEqualTo("hashed_token");
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("refresh");
        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
    }

    @Test
    void validateAccessToken_ValidAccessToken_NoException() {
        String token = sessionService.createSession(defaultUser, null).getAccessToken().getToken();
        when(revocationService.isAccessTokenBlocked(any(UUID.class), any(UUID.class), any(Instant.class))).thenReturn(false);

        assertThatNoException().isThrownBy(() -> sessionService.validateAccessToken(token));
    }

    @Test
    void validateAccessToken_BlockedToken_ThrowsInvalidTokenException() {
        String token = sessionService.createSession(defaultUser, null).getAccessToken().getToken();
        when(revocationService.isAccessTokenBlocked(any(UUID.class), any(UUID.class), any(Instant.class))).thenReturn(true);

        assertThatThrownBy(() -> sessionService.validateAccessToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAccessToken_TokenInvalid_ThrowsInvalidTokenException() {
        assertThatThrownBy(() -> sessionService.validateAccessToken("refreshToken"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateRefreshToken_AccessTokenProvided_ThrowsInvalidTokenException() {
        String accessToken = sessionService.createSession(defaultUser, null).getAccessToken().getToken();

        assertThatThrownBy(() -> sessionService.validateRefreshToken(accessToken))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void getUserFromTokenNoQuery_ValidAccessToken_ReturnsCorrectUserDto() {
        String token = sessionService.createSession(defaultUser, null).getAccessToken().getToken();

        UserDto result = jwtProvider.getUserDtoFromToken(token);

        assertThat(result.getId()).isEqualTo(defaultUser.getId());
        assertThat(result.getUsername()).isEqualTo(defaultUser.getUsername());
        assertThat(result.getEmail()).isEqualTo(defaultUser.getEmail());
    }

    @Test
    void decodeToken_MalformedToken_ThrowsInvalidTokenException() {
        assertThatThrownBy(() -> jwtProvider.decodeToken("not.a.valid.token"))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void decodeToken_WrongSecret_ThrowsInvalidTokenException() {
        JwtProvider otherProvider = new JwtProvider(timeProvider);
        ReflectionTestUtils.setField(otherProvider, "privateKeyPem", OTHER_PRIVATE_KEY);
        ReflectionTestUtils.setField(otherProvider, "publicKeyPem", OTHER_PUBLIC_KEY);
        ReflectionTestUtils.setField(otherProvider, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(otherProvider, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(otherProvider, "REFRESH_EXPIRATION_DAYS", 7);
        otherProvider.init();

        String tokenFromOtherService = otherProvider.createAccessToken(defaultUser, UUID.randomUUID()).getToken();

        assertThatThrownBy(() -> jwtProvider.decodeToken(tokenFromOtherService))
            .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void blockAccessToken_ValidToken_InvalidatesSessionTokens() {
        TokenDto access = sessionService.createSession(defaultUser, null).getAccessToken();
        when(revocationService.isAccessTokenBlocked(any(UUID.class), any(UUID.class), any(Instant.class))).thenReturn(false);

        sessionService.blockAccessToken(access.getToken());

        UUID sessionId = jwtProvider.validateAccessToken(access.getToken()).getSessionId();
        verify(revocationService).invalidateSessionTokens(sessionId);
    }

    @Test
    void revokeRefreshToken_ValidToken_InvalidatesSessionAccessTokens() {
        TokenDto refresh = sessionService.createSession(defaultUser, null).getRefreshToken();
        UUID sessionId = jwtProvider.getSessionIdFromToken(refresh.getToken());

        TokenEntity entity = TokenEntity.builder()
                .id(refresh.getId())
                .sessionId(sessionId)
                .revoked(false)
                .token("hashed_token")
                .build();

        when(jpaTokenRepository.findById(refresh.getId())).thenReturn(Optional.of(entity));
        when(hasher.verify(refresh.getToken(), entity.getToken())).thenReturn(true);

        sessionService.revokeRefreshToken(refresh.getToken());

        assertThat(entity.isRevoked()).isTrue();
        verify(revocationService).invalidateSessionTokens(sessionId);
    }

    @Test
    void revokeRefreshTokenById_ValidRequest_InvalidatesTargetSessionAccessTokens() {
        TokenDto refresh = sessionService.createSession(defaultUser, null).getRefreshToken();
        UUID sessionId = jwtProvider.getSessionIdFromToken(refresh.getToken());
        UserEntity user = UserEntity.builder().id(defaultUser.getId()).build();

        TokenEntity userTokenEntity = TokenEntity.builder()
                .id(refresh.getId())
                .sessionId(sessionId)
                .revoked(false)
                .token("hashed_token")
                .user(user)
                .build();

        UUID targetSessionId = UUID.randomUUID();
        TokenEntity targetEntity = TokenEntity.builder()
                .id(UUID.randomUUID())
                .sessionId(targetSessionId)
                .revoked(false)
                .user(user)
                .build();

        when(jpaTokenRepository.findById(refresh.getId())).thenReturn(Optional.of(userTokenEntity));
        when(jpaTokenRepository.findById(targetEntity.getId())).thenReturn(Optional.of(targetEntity));
        when(hasher.verify(refresh.getToken(), userTokenEntity.getToken())).thenReturn(true);

        sessionService.revokeRefreshTokenById(refresh.getToken(), targetEntity.getId());

        assertThat(targetEntity.isRevoked()).isTrue();
        verify(revocationService).invalidateSessionTokens(targetSessionId);
    }

    @Test
    void revokeAllUserTokens_ByUserId_MarksAllAsRevokedAndInvalidatesAccessTokens() {
        UUID userId = UUID.randomUUID();
        TokenEntity first = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();
        TokenEntity second = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();

        when(jpaTokenRepository.findAllByUserId(userId)).thenReturn(List.of(first, second));

        sessionService.revokeAllUserTokens(userId);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(revocationService).invalidateAllUserTokens(userId);
    }
}
