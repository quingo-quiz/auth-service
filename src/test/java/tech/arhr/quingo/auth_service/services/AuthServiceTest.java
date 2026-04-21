package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.exceptions.auth.AccountNotActiveException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;
import tech.arhr.quingo.auth_service.api.rest.models.ChangePasswordRequest;
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

    @Mock
    private MfaService mfaService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(tokenService, tokenMapper, userService,  verificationService, socialAccountService, mfaService);
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
        void authenticate_MfaEnabledUser_ReturnsMfaChallenge() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        UserDto user = UserDto.builder()
            .id(UUID.randomUUID())
            .accountStatus(AccountStatus.ACTIVE)
            .mfaEnabled(true)
            .build();
        TokenDto mfaToken = TokenDto.builder().id(UUID.randomUUID()).token("mfa-token").build();

        when(userService.checkPasswordReturnUser(request.getEmail(), request.getPassword())).thenReturn(user);
        when(tokenService.createMfaTempToken(user)).thenReturn(mfaToken);

        AuthResponse result = authService.authenticate(request);

        assertThat(result.isMfaRequired()).isTrue();
        assertThat(result.getMfaTempToken()).isEqualTo(mfaToken);
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRefreshToken()).isNull();
        verify(tokenService, never()).createAccessToken(any());
        verify(tokenService, never()).createRefreshToken(any());
        }

        @Test
        void authenticate_NonActiveUser_ThrowsAccountNotActiveException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        UserDto user = UserDto.builder()
            .id(UUID.randomUUID())
            .accountStatus(AccountStatus.BLOCKED)
            .mfaEnabled(false)
            .build();

        when(userService.checkPasswordReturnUser(request.getEmail(), request.getPassword())).thenReturn(user);

        assertThatThrownBy(() -> authService.authenticate(request))
            .isInstanceOf(AccountNotActiveException.class)
            .hasMessageContaining("BLOCKED");
        }

        @Test
        void verifyOtpIssueTokens_ValidRequest_VerifiesOtpAndReturnsTokenPair() {
        UUID userId = UUID.randomUUID();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("123456");
        request.setMfaTempToken("mfa-temp-token");

        UserDto user = UserDto.builder().id(userId).build();
        TokenDto access = TokenDto.builder().id(UUID.randomUUID()).token("access").build();
        TokenDto refresh = TokenDto.builder().id(UUID.randomUUID()).token("refresh").build();

        when(tokenService.validateMfaTempToken(request.getMfaTempToken())).thenReturn(userId);
        when(userService.getUserById(userId)).thenReturn(user);
        when(tokenService.createAccessToken(user)).thenReturn(access);
        when(tokenService.createRefreshToken(user)).thenReturn(refresh);

        AuthResponse response = authService.verifyOtpIssueTokens(request);

        assertThat(response.getAccessToken()).isEqualTo(access);
        assertThat(response.getRefreshToken()).isEqualTo(refresh);
        verify(mfaService).verifyOtpCode(userId, request);
        }

        @Test
        void changePassword_ValidRequest_UpdatesPasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass123");
        request.setNewPassword("newPass123");

        authService.changePassword(userId, request);

        verify(userService).checkPasswordReturnUser(userId, "oldPass123");
        verify(userService).updateUserPassword(userId, "newPass123");
        verify(tokenService).revokeAllUserTokens(userId);
        }

        @Test
        void processOAuth2User_ExistingSocialAccount_ReturnsLinkedUser() {
        OAuth2UserData userData = OAuth2UserData.builder()
            .provider(OAuth2Provider.GITHUB)
            .providerUserId("provider-id")
            .email("user@test.com")
            .emailVerified(true)
            .username("username")
            .build();

        UUID userId = UUID.randomUUID();
        SocialAccountDto account = new SocialAccountDto();
        account.setUserId(userId);
        account.setProvider(OAuth2Provider.GITHUB);
        account.setProviderUserId("provider-id");
        UserDto user = UserDto.builder().id(userId).build();

        when(socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GITHUB, "provider-id")).thenReturn(account);
        when(userService.getUserById(userId)).thenReturn(user);

        UserDto result = authService.processOAuth2User(userData);

        assertThat(result).isEqualTo(user);
        verify(userService, never()).createUserFromOAuth2(any());
        verify(socialAccountService, never()).linkSocialAccount(any(), any());
        }

        @Test
        void processOAuth2User_NewProviderUser_ExistingLocalUnverifiedUser_ClearsPasswordAndRevokesTokens() {
        OAuth2UserData userData = OAuth2UserData.builder()
            .provider(OAuth2Provider.GOOGLE)
            .providerUserId("google-id")
            .email("user@test.com")
            .emailVerified(true)
            .username("username")
            .build();

        UserDto localUser = UserDto.builder()
            .id(UUID.randomUUID())
            .email("user@test.com")
            .emailVerified(false)
            .build();

        when(socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GOOGLE, "google-id"))
            .thenThrow(new EntityNotFoundException("Social account not found"));
        when(userService.getUserByEmail("user@test.com")).thenReturn(localUser);

        UserDto result = authService.processOAuth2User(userData);

        assertThat(result).isEqualTo(localUser);
        verify(userService).clearUserPassword(localUser.getId());
        verify(tokenService).revokeAllUserTokens(localUser.getId());
        verify(socialAccountService).linkSocialAccount(userData, localUser.getId());
        }

        @Test
        void processOAuth2User_NewProviderUser_NewLocalUser_CreatesAndLinks() {
        OAuth2UserData userData = OAuth2UserData.builder()
            .provider(OAuth2Provider.GOOGLE)
            .providerUserId("google-id")
            .email("new@test.com")
            .emailVerified(true)
            .username("new-user")
            .build();

        UserDto created = UserDto.builder().id(UUID.randomUUID()).build();

        when(socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GOOGLE, "google-id"))
            .thenThrow(new EntityNotFoundException("Social account not found"));
        when(userService.getUserByEmail("new@test.com"))
            .thenThrow(new EntityNotFoundException("User not found"));
        when(userService.createUserFromOAuth2(userData)).thenReturn(created);

        UserDto result = authService.processOAuth2User(userData);

        assertThat(result).isEqualTo(created);
        verify(socialAccountService).linkSocialAccount(userData, created.getId());
        }

        @Test
        void processOAuth2User_UnverifiedProviderEmailForExistingUser_ThrowsOAuth2AuthenticationException() {
        OAuth2UserData userData = OAuth2UserData.builder()
            .provider(OAuth2Provider.GOOGLE)
            .providerUserId("google-id")
            .email("user@test.com")
            .emailVerified(false)
            .username("username")
            .build();

        UserDto localUser = UserDto.builder().id(UUID.randomUUID()).emailVerified(true).build();

        when(socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GOOGLE, "google-id"))
            .thenThrow(new EntityNotFoundException("Social account not found"));
        when(userService.getUserByEmail("user@test.com")).thenReturn(localUser);

        assertThatThrownBy(() -> authService.processOAuth2User(userData))
            .isInstanceOf(OAuth2AuthenticationException.class)
            .hasMessageContaining("unverified provider account email");

        verify(socialAccountService, never()).linkSocialAccount(any(), any());
        }
}