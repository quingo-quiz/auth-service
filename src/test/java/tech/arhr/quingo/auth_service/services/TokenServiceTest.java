package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.arhr.quingo.auth_service.api.security.ClientContext;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

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
    private ClientContext clientContext;

    @InjectMocks
    private TokenService tokenService;

    private UserDto defaultUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "JWT_SECRET", "test-secret-key");
        ReflectionTestUtils.setField(tokenService, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(tokenService, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(tokenService, "REFRESH_EXPIRATION_DAYS", 7);
        tokenService.init();

        defaultUser = UserDto.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .roles(List.of(UserRole.USER))
                .build();

        lenient().when(timeProvider.now()).thenReturn(Instant.now());
        clientContext = new ClientContext();
    }


    @Test
    void createAccessToken_NullRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(null)
                .build();

        assertThatThrownBy(() -> tokenService.createAccessToken(user))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_EmptyRoles_ThrowsInvalidTokenException() {
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .roles(List.of())
                .build();

        assertThatThrownBy(() -> tokenService.createAccessToken(user))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void createAccessToken_ValidUser_TokenContainsCorrectClaims() {
        TokenDto result = tokenService.createAccessToken(defaultUser);
        DecodedJWT decoded = tokenService.decodeToken(result.getToken());

        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("access");
        assertThat(decoded.getClaim("username").asString()).isEqualTo(defaultUser.getUsername());
        assertThat(decoded.getClaim("email").asString()).isEqualTo(defaultUser.getEmail());
        assertThat(result.getExpiresAt()).isAfter(result.getIssuedAt());
    }

    @Test
    void createAccessToken_ValidUser_RegistersTokenInWhiteList() {
        tokenService.createAccessToken(defaultUser);

        verify(whiteListTokenService).registerToken(any(TokenDto.class));
    }


    @Test
    void createRefreshToken_ValidUser_SavesHashedTokenAndReturnsDto() {
        TokenEntity entity = new TokenEntity();
        when(tokenMapper.toEntity(any(TokenDto.class))).thenReturn(entity);
        when(hasher.hash(any())).thenReturn("hashed_token");

        TokenDto result = tokenService.createRefreshToken(defaultUser);
        DecodedJWT decoded = tokenService.decodeToken(result.getToken());

        verify(jpaTokenRepository).save(entity);
        assertThat(entity.getToken()).isEqualTo("hashed_token");
        assertThat(decoded.getClaim("typ").asString()).isEqualTo("refresh");
        assertThat(decoded.getSubject()).isEqualTo(defaultUser.getId().toString());
    }

    @Test
    void validateAccessToken_ValidAccessToken_NoException() {
        String token = tokenService.createAccessToken(defaultUser).getToken();
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(false);

        assertThatNoException().isThrownBy(() -> tokenService.validateAccessToken(token));
    }

    @Test
    void validateAccessToken_BlockedToken_ThrowsInvalidTokenException() {
        String token = tokenService.createAccessToken(defaultUser).getToken();
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(true);

        assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateAccessToken_TokenInvalid_ThrowsInvalidTokenException() {
        assertThatThrownBy(() -> tokenService.validateAccessToken("refreshToken"))
                .isInstanceOf(InvalidTokenException.class);
    }


    @Test
    void validateRefreshToken_AccessTokenProvided_ThrowsInvalidTokenException() {
        String accessToken = tokenService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> tokenService.validateRefreshToken(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void getUserFromTokenNoQuery_ValidAccessToken_ReturnsCorrectUserDto() {
        String token = tokenService.createAccessToken(defaultUser).getToken();

        UserDto result = tokenService.getUserFromTokenNoQuery(token);

        assertThat(result.getId()).isEqualTo(defaultUser.getId());
        assertThat(result.getUsername()).isEqualTo(defaultUser.getUsername());
        assertThat(result.getEmail()).isEqualTo(defaultUser.getEmail());
    }


    @Test
    void decodeToken_MalformedToken_ThrowsInvalidTokenException() {
        assertThatThrownBy(() -> tokenService.decodeToken("not.a.valid.token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void decodeToken_WrongSecret_ThrowsInvalidTokenException() {
        TokenService otherService = new TokenService(jpaTokenRepository, whiteListTokenService, userService, hasher, tokenMapper, timeProvider, clientContext);
        ReflectionTestUtils.setField(otherService, "JWT_SECRET", "other-secret");
        ReflectionTestUtils.setField(otherService, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(otherService, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(otherService, "REFRESH_EXPIRATION_DAYS", 7);
        otherService.init();

        String tokenFromOtherService = otherService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> tokenService.decodeToken(tokenFromOtherService))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateMfaTempToken_TokkenValid_ReturnsUserId() {
        TokenDto token = tokenService.createMfaTempToken(defaultUser);

        UUID userId = tokenService.validateMfaTempToken(token.getToken());

        assertThat(userId).isEqualTo(defaultUser.getId());
    }

    @Test
    void validateMfaTempToken_WithAccessToken_ThrowsInvalidTokenException() {
        String accessToken = tokenService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> tokenService.validateMfaTempToken(accessToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid token type");
    }

    @Test
    void blockAccessToken_ValidToken_DelegatesToWhiteListBlock() {
        TokenDto access = tokenService.createAccessToken(defaultUser);
        when(whiteListTokenService.isBlocked(any(UUID.class))).thenReturn(false);

        tokenService.blockAccessToken(access.getToken());

        verify(whiteListTokenService).blockToken(access.getId());
    }

    @Test
    void revokeAllUserTokens_ByUserId_MarksAllAsRevokedAndBlocksWhitelist() {
        UUID userId = UUID.randomUUID();
        TokenEntity first = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();
        TokenEntity second = TokenEntity.builder().id(UUID.randomUUID()).revoked(false).build();

        when(jpaTokenRepository.findAllByUserId(userId)).thenReturn(List.of(first, second));

        tokenService.revokeAllUserTokens(userId);

        assertThat(first.isRevoked()).isTrue();
        assertThat(second.isRevoked()).isTrue();
        verify(whiteListTokenService).blockAllUserTokens(userId);
    }
}