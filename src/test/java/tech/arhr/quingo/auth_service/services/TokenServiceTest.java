package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        TokenService otherService = new TokenService(jpaTokenRepository, userService, hasher, tokenMapper);
        ReflectionTestUtils.setField(otherService, "JWT_SECRET", "other-secret");
        ReflectionTestUtils.setField(otherService, "ISSUER", "test-issuer");
        ReflectionTestUtils.setField(otherService, "ACCESS_EXPIRATION_MINUTES", 15);
        ReflectionTestUtils.setField(otherService, "REFRESH_EXPIRATION_DAYS", 7);
        otherService.init();

        String tokenFromOtherService = otherService.createAccessToken(defaultUser).getToken();

        assertThatThrownBy(() -> tokenService.decodeToken(tokenFromOtherService))
                .isInstanceOf(InvalidTokenException.class);
    }
}