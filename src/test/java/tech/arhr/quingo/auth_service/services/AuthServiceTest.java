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
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.JwtProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;
import tech.arhr.quingo.auth_service.dto.auth.SessionTokens;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SessionService sessionService;

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

    @Mock
    private Hasher hasher;

    @Mock
    private JwtProvider jwtProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(sessionService, tokenMapper, userService, verificationService, socialAccountService, mfaService, hasher, jwtProvider);
    }

    @Test
    void refresh_ValidToken_RevokesOldToken() {
        String refreshToken = "refresh-token";
        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .build();

                when(jwtProvider.getUserIdFromToken(refreshToken)).thenReturn(user.getId());
                when(userService.getUserById(user.getId())).thenReturn(user);
                when(sessionService.createSession(eq(user), any())).thenReturn(new SessionTokens(TokenDto.builder().build(), TokenDto.builder().build()));

                authService.refresh(refreshToken);

                verify(sessionService).revokeRefreshToken(refreshToken);
    }

    @Test
    void refresh_ValidToken_ReturnsNewTokenPair() {
        String refreshToken = "refresh-token";
        UserDto user = UserDto.builder().id(UUID.randomUUID()).build();
        TokenDto newAccess = TokenDto.builder().id(UUID.randomUUID()).build();
        TokenDto newRefresh = TokenDto.builder().id(UUID.randomUUID()).build();

                when(jwtProvider.getUserIdFromToken(refreshToken)).thenReturn(user.getId());
                when(userService.getUserById(user.getId())).thenReturn(user);
                when(sessionService.createSession(eq(user), any())).thenReturn(new SessionTokens(newAccess, newRefresh));

                AuthResponse result = authService.refresh(refreshToken);

                assertThat(result.getAccessToken()).isEqualTo(newAccess);
                assertThat(result.getRefreshToken()).isEqualTo(newRefresh);
    }

    @Test
    void refresh_InvalidToken_ThrowsInvalidTokenException() {
        when(sessionService.validateRefreshToken(anyString())).thenThrow(new InvalidTokenException());

        assertThatThrownBy(() -> authService.refresh("token"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void authorize_ValidToken_ReturnsUserDto() {
        String accessToken = "access-token";
        UserDto expected = UserDto.builder().id(UUID.randomUUID()).build();
                when(sessionService.validateAccessToken(accessToken)).thenReturn(UUID.randomUUID());
                when(jwtProvider.getUserDtoFromToken(accessToken)).thenReturn(expected);

                UserDto result = authService.authorize(accessToken);

                assertThat(result).isEqualTo(expected);
    }

    @Test
    void authorize_InvalidToken_ThrowsInvalidTokenException() {
        String accessToken = "token";
        doThrow(new InvalidTokenException()).when(sessionService).validateAccessToken(accessToken);

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
                .hashedPassword("password")
                .build();
        TokenDto mfaToken = TokenDto.builder().id(UUID.randomUUID()).token("mfa-token").build();

        when(userService.getUserByEmail(request.getEmail())).thenReturn(user);
        when(jwtProvider.createMfaTempToken(user)).thenReturn(mfaToken);
        when(mfaService.isMfaEnabledForUser(user.getId())).thenReturn(true);
        when(hasher.verify(anyString(), anyString())).thenReturn(true);

        AuthResponse result = authService.authenticate(request);

        assertThat(result.isMfaRequired()).isTrue();
        assertThat(result.getMfaTempToken()).isEqualTo(mfaToken);
        assertThat(result.getAccessToken()).isNull();
        assertThat(result.getRefreshToken()).isNull();
                verify(sessionService, never()).createSession(any(), any());
    }

    @Test
    void authenticate_NonActiveUser_ThrowsAccountNotActiveException() {
        AuthRequest request = new AuthRequest();
        request.setEmail("user@test.com");
        request.setPassword("password");

        UserDto user = UserDto.builder()
                .id(UUID.randomUUID())
                .accountStatus(AccountStatus.BLOCKED)
                .build();

        when(userService.getUserByEmail(request.getEmail())).thenReturn(user);

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
                when(jwtProvider.validateMfaTempToken(request.getMfaTempToken())).thenReturn(userId);
                when(userService.getUserById(userId)).thenReturn(user);
                when(sessionService.createSession(eq(user), any())).thenReturn(new SessionTokens(access, refresh));

                AuthResponse response = authService.verifyOtpIssueTokens(request);

                assertThat(response.getAccessToken()).isEqualTo(access);
                assertThat(response.getRefreshToken()).isEqualTo(refresh);
                verify(mfaService).verifyOtpCode(userId, request.getCode());
    }

    @Test
    void changePassword_ValidRequest_UpdatesPasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        UserDto dto =  UserDto.builder()
                .id(userId)
                .hashedPassword("hash")
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("oldPass");
        request.setNewPassword("newPass");

        when(userService.getUserById(userId)).thenReturn(dto);
        when(hasher.verify("oldPass", "hash")).thenReturn(true);

        authService.changePassword(userId, request.getOldPassword(), request.getNewPassword());

        verify(userService).updateUserPassword(userId, "newPass");
        verify(sessionService).revokeAllUserTokens(userId);
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
        verify(sessionService).revokeAllUserTokens(localUser.getId());
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

    @Test
    void setPassword_Valid_UpdatesPasswordAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        String password = "newPassword";

        when(userService.isPasswordSetForUser(userId)).thenReturn(false);

        authService.setPassword(userId, password);

        verify(userService).updateUserPassword(userId, password);
        verify(sessionService).revokeAllUserTokens(userId);
    }
}