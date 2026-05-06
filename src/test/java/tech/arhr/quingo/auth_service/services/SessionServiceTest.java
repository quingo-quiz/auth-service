package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private JpaTokenRepository jpaTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private Hasher hasher;

    @Mock
    private TokenMapper tokenMapper;

    @Mock
    private TimeProvider timeProvider;

    @Mock
    private WhiteListTokenService whiteListTokenService;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private SessionService sessionService;

    private UserDto defaultUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionService, "JWT_SECRET", "test-secret-key");
        ReflectionTestUtils.setField(sessionService, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(sessionService, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(sessionService, "REFRESH_EXPIRATION_DAYS", 7);
        sessionService.init();

        defaultUser = UserDto.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .roles(List.of(UserRole.USER))
                .build();

        lenient().when(timeProvider.now()).thenReturn(Instant.now());
    }


    @Test
    void createAccessToken_NullRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(null)
                .build();

        assertThatThrownBy(() -> sessionService.createAccessToken(user))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_EmptyRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(List.of())
                .build();

        assertThatThrownBy(() -> sessionService.createAccessToken(user))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_ValidUser_TokenContainsCorrectClaims() {
        TokenDto result = sessionService.createAccessToken(defaultUser);
        DecodedJWT decoded = sessionService.decodeToken(result.getToken());

        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("access");
        assertThat(decoded.getClaim("username").asString()).isEqualTo(defaultUser.getUsername());
        assertThat(decoded.getClaim("email").asString()).isEqualTo(defaultUser.getEmail());
        assertThat(result.getExpiresAt()).isAfter(result.getIssuedAt());
    }

    @Test
    void createAccessToken_ValidUser_RegistersTokenInWhiteList() {
        sessionService.createAccessToken(defaultUser);

        verify(whiteListTokenService).registerToken(any(TokenDto.class));
    }


    @Test
    void createRefreshToken_ValidUser_SavesHashedTokenAndReturnsDto() {
        TokenEntity entity = new TokenEntity();
        when(tokenMapper.toEntity(any(TokenDto.class))).thenReturn(entity);
        when(hasher.hash(any())).thenReturn("hashed_token");

        TokenDto result = sessionService.createRefreshToken(defaultUser);
        DecodedJWT decoded = sessionService.decodeToken(result.getToken());

        verify(jpaTokenRepository).save(entity);
        assertThat(entity.getToken()).isEqualTo("hashed_token");
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("refresh");
        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
    }

    @Test
    void validateAccessToken_ValidAccessToken_NoException() {
        String token = sessionService.createAccessToken(defaultUser).getToken();
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(false);

        assertThatNoException().isThrownBy(() -> sessionService.validateAccessToken(token));
    }

    @Test
    void validateAccessToken_BlockedToken_ThrowsInvalidTokenException() {
        String token = sessionService.createAccessToken(defaultUser).getToken();
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(true);

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
        String accessToken = sessionService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> sessionService.validateRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void getUserFromTokenNoQuery_ValidAccessToken_ReturnsCorrectUserDto() {
        String token = sessionService.createAccessToken(defaultUser).getToken();

        UserDto result = sessionService.getUserFromTokenNoQuery(token);

        assertThat(result.getId()).isEqualTo(defaultUser.getId());
        assertThat(result.getUsername()).isEqualTo(defaultUser.getUsername());
        assertThat(result.getEmail()).isEqualTo(defaultUser.getEmail());
    }


    @Test
    void decodeToken_MalformedToken_ThrowsInvalidTokenException() {
        assertThatThrownBy(() -> sessionService.decodeToken("not.a.valid.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void decodeToken_WrongSecret_ThrowsInvalidTokenException() {
        SessionService otherService = new SessionService(jpaTokenRepository, whiteListTokenService, userService, hasher, tokenMapper, timeProvider);
        ReflectionTestUtils.setField(otherService, "JWT_SECRET", "other-secret");
        ReflectionTestUtils.setField(otherService, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(otherService, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(otherService, "REFRESH_EXPIRATION_DAYS", 7);
        otherService.init();

        String tokenFromOtherService = otherService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> sessionService.decodeToken(tokenFromOtherService))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void blockAccessToken_ValidToken_DelegatesToWhiteListBlock() {
        TokenDto access = sessionService.createAccessToken(defaultUser);
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(false);

        sessionService.blockAccessToken(access.getToken());

        verify(whiteListTokenService).blockToken(access.getId());
    }

    @Test
    void revokeAllUserTokens_ByUserId_MarksAllAsRevokedAndBlocksWhitelist() {
        UUID userId = UUID.randomUUID();
        TokenEntity first = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();
        TokenEntity second = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();

        when(jpaTokenRepository.findAllByUserId(userId)).thenReturn(List.of(first, second));

        sessionService.revokeAllUserTokens(userId);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(whiteListTokenService).blockAllUserTokens(userId);
    }
}