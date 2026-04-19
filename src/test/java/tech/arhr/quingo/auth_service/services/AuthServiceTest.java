package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private TokenMapper tokenMapper;

    @Mock
    private UserService userService;

    @Mock
    private SocialAccountService socialAccountService;

    @Mock
    private VerificationService verificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(tokenService, tokenMapper, userService,  verificationService, socialAccountService);
    }

    @Test
    void refresh_ValidToken_RevokesOldToken() {
        String refreshToken = "refresh-token";
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .build();

        when(tokenService.getUserFromTokenWithQuery(refreshToken)).thenReturn(user);
        when(tokenService.createAccessToken(user)).thenReturn(TokenDto.builder().build());
        when(tokenService.createRefreshToken(user)).thenReturn(TokenDto.builder().build());

        authService.refresh(refreshToken);

        verify(tokenService).revokeRefreshToken(refreshToken);
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokenPair() {
        String refreshToken = "refresh-token";
        UserDto user = UserDto.builder().id(UUID.randomUUID()).build();
        TokenDto newAccess = TokenDto.builder().id(UUID.randomUUID()).build();
        TokenDto newRefresh = TokenDto.builder().id(UUID.randomUUID()).build();

        when(tokenService.getUserFromTokenWithQuery(refreshToken)).thenReturn(user);
        when(tokenService.createAccessToken(user)).thenReturn(newAccess);
        when(tokenService.createRefreshToken(user)).thenReturn(newRefresh);

        AuthResponse result = authService.refresh(refreshToken);

        assertThat(result.getAccessToken()).isEqualTo(newAccess);
        assertThat(result.getRefreshToken()).isEqualTo(newRefresh);
    }

    @Test
    void refresh_InvalidToken_ThrowsInvalidTokenException() {
        when(tokenService.getUserFromTokenWithQuery(any())).thenThrow(new InvalidTokenException());

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void authorize_ValidToken_ReturnsUserDto() {
        String accessToken = "access-token";
        UserDto expected = UserDto.builder().id(UUID.randomUUID()).build();

        when(tokenService.getUserFromTokenNoQuery(accessToken)).thenReturn(expected);

        UserDto result = authService.authorize(accessToken);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void authorize_InvalidToken_ThrowsInvalidTokenException() {
        String accessToken = "token";
        doThrow(new InvalidTokenException()).when(tokenService).validateAccessToken(accessToken);

        assertThatThrownBy(() -> authService.authorize(accessToken))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void authenticate_UnknownProviderString_ThrowsAuthException() {
        AuthRequest request = new AuthRequest();

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void register_UnknownProviderString_ThrowsAuthException() {
        RegisterRequest request = new RegisterRequest();

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AuthException.class);
    }
}