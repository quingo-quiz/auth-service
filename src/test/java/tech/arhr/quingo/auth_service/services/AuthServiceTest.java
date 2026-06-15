package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.exceptions.auth.AccountNotActiveException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.api.rest.models.ChangePasswordRequest;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.JwtProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;
import tech.arhr.quingo.auth_service.dto.auth.SessionTokens;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import org.mockito.ArgumentCaptor;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.auth.PermissionDeniedException;
import tech.arhr.quingo.auth_service.exceptions.auth.PasswordNotSetException;
import tech.arhr.quingo.auth_service.api.rest.models.RefreshTokenApiModel;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        private MfaService mfaService;

        @Mock
        private Hasher hasher;

        @Mock
        private JwtProvider jwtProvider;

        @Mock
        private ApplicationEventPublisher publisher;

        private AuthService authService;

        @BeforeEach
        void setUp() {
                authService = new AuthService(sessionService, tokenMapper, userService, mfaService, hasher, jwtProvider, publisher);
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

                authService.refresh(refreshToken, new UserAgentInfoDto());

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

                AuthResponse result = authService.refresh(refreshToken, new UserAgentInfoDto());

                assertThat(result.getAccessToken()).isEqualTo(newAccess);
                assertThat(result.getRefreshToken()).isEqualTo(newRefresh);
        }

        @Test
        void refresh_InvalidToken_ThrowsInvalidTokenException() {
                when(sessionService.validateRefreshToken(anyString())).thenThrow(new InvalidTokenException());

                assertThatThrownBy(() -> authService.refresh("token", new UserAgentInfoDto()))
                                .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        void authorize_ValidToken_ReturnsUserDto() {
                String accessToken = "access-token";
                UserDto expected = UserDto.builder().id(UUID.randomUUID()).build();
                when(sessionService.validateAccessToken(accessToken)).thenReturn(TokenDto.builder().build());
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

                AuthResponse result = authService.authenticate(request, new UserAgentInfoDto());

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

                assertThatThrownBy(() -> authService.authenticate(request, new UserAgentInfoDto()))
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

                AuthResponse response = authService.verifyOtpIssueTokens(request, new UserAgentInfoDto());

                assertThat(response.getAccessToken()).isEqualTo(access);
                assertThat(response.getRefreshToken()).isEqualTo(refresh);
                verify(mfaService).verifyOtpCode(userId, request.getCode());
        }

        @Test
        void changePassword_ValidRequest_UpdatesPasswordAndPublishesEvent() {
                UUID userId = UUID.randomUUID();
                UserDto dto = UserDto.builder()
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
                ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(publisher).publishEvent(captor.capture());
                assertThat(((AllUserSessionsInvalidatedEvent) captor.getValue()).userId()).isEqualTo(userId);
        }

        @Test
        void setPassword_Valid_UpdatesPasswordAndPublishesEvent() {
                UUID userId = UUID.randomUUID();
                String password = "newPassword";

                when(userService.isPasswordSetForUser(userId)).thenReturn(false);

                authService.setPassword(userId, password);

                verify(userService).updateUserPassword(userId, password);
                ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(publisher).publishEvent(captor.capture());
                assertThat(((AllUserSessionsInvalidatedEvent) captor.getValue()).userId()).isEqualTo(userId);
        }

        @Test
        void register_Success_CreatesUserAndSessionAndPublishesEvent() {
                RegisterRequest request = new RegisterRequest();
                UserDto user = UserDto.builder().id(UUID.randomUUID()).build();
                TokenDto access = TokenDto.builder().id(UUID.randomUUID()).token("access").build();
                TokenDto refresh = TokenDto.builder().id(UUID.randomUUID()).token("refresh").build();

                when(userService.createUser(request)).thenReturn(user);
                when(sessionService.createSession(eq(user), any())).thenReturn(new SessionTokens(access, refresh));

                AuthResponse response = authService.register(request, new UserAgentInfoDto());

                assertThat(response.getAccessToken()).isEqualTo(access);
                assertThat(response.getRefreshToken()).isEqualTo(refresh);
                ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
                verify(publisher).publishEvent(captor.capture());
                assertThat(captor.getValue()).isInstanceOf(tech.arhr.quingo.auth_service.events.user.UserRegisteredEvent.class);
        }

        @Test
        void authenticate_InvalidPassword_ThrowsInvalidCredentialsException() {
                AuthRequest request = new AuthRequest();
                request.setEmail("user@test.com");
                request.setPassword("wrong");

                UserDto user = UserDto.builder().id(UUID.randomUUID()).accountStatus(AccountStatus.ACTIVE).hashedPassword("hash").build();

                when(userService.getUserByEmail(request.getEmail())).thenReturn(user);
                when(hasher.verify(anyString(), anyString())).thenReturn(false);

                assertThatThrownBy(() -> authService.authenticate(request, new UserAgentInfoDto()))
                                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void changePassword_InvalidOldPassword_ThrowsInvalidCredentialsException() {
                UUID userId = UUID.randomUUID();
                UserDto dto = UserDto.builder().id(userId).hashedPassword("hash").build();

                when(userService.getUserById(userId)).thenReturn(dto);
                when(hasher.verify(anyString(), anyString())).thenReturn(false);

                assertThatThrownBy(() -> authService.changePassword(userId, "old", "new"))
                                .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void setPassword_WhenAlreadySet_ThrowsPermissionDeniedException() {
                UUID userId = UUID.randomUUID();
                when(userService.isPasswordSetForUser(userId)).thenReturn(true);

                assertThatThrownBy(() -> authService.setPassword(userId, "pwd"))
                                .isInstanceOf(PermissionDeniedException.class);
        }

        @Test
        void logout_CallsRevokeAndBlock() {
                String refresh = "refresh";
                String access = "access";

                authService.logout(refresh, access);

                verify(sessionService).revokeRefreshToken(refresh);
                verify(sessionService).blockAccessToken(access);
        }

        @Test
        void logoutTokenById_CallsRevokeById() {
                String refresh = "refresh";
                UUID tokenId = UUID.randomUUID();

                authService.logoutTokenById(refresh, tokenId);

                verify(sessionService).revokeRefreshTokenById(refresh, tokenId);
        }

        @Test
        void logoutAll_CallsRevokeAllUserTokens() {
                String refresh = "refresh";

                authService.logoutAll(refresh);

                verify(sessionService).revokeAllUserTokens(refresh);
        }

        @Test
        void getActiveRefreshTokens_MarksCurrentToken() {
                UUID userId = UUID.randomUUID();
                UUID currentSessionId = UUID.randomUUID();
                String accessToken = "access-token";

                TokenDto t1 = TokenDto.builder().id(UUID.randomUUID()).sessionId(currentSessionId).build();
                TokenDto t2 = TokenDto.builder().id(UUID.randomUUID()).sessionId(UUID.randomUUID()).build();

                RefreshTokenApiModel m1 = new RefreshTokenApiModel();
                m1.setSessionId(currentSessionId);
                RefreshTokenApiModel m2 = new RefreshTokenApiModel();
                m2.setSessionId(t2.getSessionId());

                when(jwtProvider.getSessionIdFromToken(accessToken)).thenReturn(currentSessionId);
                when(sessionService.getActiveRefreshTokens(userId)).thenReturn(List.of(t1, t2));
                when(tokenMapper.toApiModel(t1)).thenReturn(m1);
                when(tokenMapper.toApiModel(t2)).thenReturn(m2);

                List<RefreshTokenApiModel> result = authService.getActiveRefreshTokens(userId, accessToken);

                assertThat(result).hasSize(2);
                assertThat(result.stream().filter(RefreshTokenApiModel::isCurrent).count()).isEqualTo(1);
        }
}