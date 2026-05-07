package tech.arhr.quingo.auth_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.api.rest.models.RefreshTokenApiModel;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.*;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import tech.arhr.quingo.auth_service.events.user.UserRegisteredEvent;
import tech.arhr.quingo.auth_service.exceptions.auth.AccountNotActiveException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.auth.PasswordNotSetException;
import tech.arhr.quingo.auth_service.exceptions.auth.PermissionDeniedException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.JwtProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {
    private final SessionService sessionService;
    private final UserService userService;
    private final MfaService mfaService;
    private final TokenMapper tokenMapper;
    private final JwtProvider jwtProvider;
    private final Hasher hasher;
    private final ApplicationEventPublisher publisher;

    public AuthService(
            SessionService sessionService,
            TokenMapper tokenMapper,
            UserService userService,
            MfaService mfaService,
            Hasher hasher,
            JwtProvider jwtProvider, ApplicationEventPublisher publisher
    ) {
        this.sessionService = sessionService;
        this.userService = userService;
        this.tokenMapper = tokenMapper;
        this.mfaService = mfaService;
        this.jwtProvider = jwtProvider;
        this.hasher = hasher;
        this.publisher = publisher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, UserAgentInfoDto agentInfo) {
        UserDto user = userService.createUser(request);
        publisher.publishEvent(new UserRegisteredEvent(user));

        SessionTokens tokens = sessionService.createSession(user, agentInfo);
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request, UserAgentInfoDto agentInfo) {
        try {
            UserDto user = userService.getUserByEmail(request.getEmail());

            if (user.getAccountStatus() != AccountStatus.ACTIVE) {
                throw new AccountNotActiveException("Account status is " + user.getAccountStatus());
            }

            checkPassword(request.getPassword(), user.getHashedPassword());

            if (mfaService.isMfaEnabledForUser(user.getId())) {
                return AuthResponse.builder()
                        .mfaTempToken(jwtProvider.createMfaTempToken(user))
                        .mfaRequired(true)
                        .build();
            }

            SessionTokens tokens = sessionService.createSession(user, agentInfo);
            return AuthResponse.builder()
                    .accessToken(tokens.getAccessToken())
                    .refreshToken(tokens.getRefreshToken())
                    .build();

        } catch (EntityNotFoundException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse verifyOtpIssueTokens(OtpVerifyRequest request, UserAgentInfoDto agentInfo) {
        UUID userId = jwtProvider.validateMfaTempToken(request.getMfaTempToken());
        mfaService.verifyOtpCode(userId, request.getCode());

        UserDto user = userService.getUserById(userId);

        SessionTokens tokens = sessionService.createSession(user, agentInfo);
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken, UserAgentInfoDto agentInfo) {
        sessionService.validateRefreshToken(refreshToken);
        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        UserDto user = userService.getUserById(userId);

        sessionService.revokeRefreshToken(refreshToken);

        SessionTokens tokens = sessionService.createSession(user, agentInfo);
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        sessionService.revokeRefreshToken(refreshToken);
        if (accessToken != null) {
            sessionService.blockAccessToken(accessToken);
        }
    }

    @Transactional
    public void logoutTokenById(String refreshToken, UUID tokenId) {
        sessionService.revokeRefreshTokenById(refreshToken, tokenId);
    }

    @Transactional
    public void logoutAll(String refreshToken) {
        sessionService.revokeAllUserTokens(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserDto authorize(String accessToken) {
        sessionService.validateAccessToken(accessToken);
        return jwtProvider.getUserDtoFromToken(accessToken);
    }

    @Transactional(readOnly = true)
    public List<RefreshTokenApiModel> getActiveRefreshTokens(UUID userId, String accessToken) {
        UUID sessionId = jwtProvider.getSessionIdFromToken(accessToken);

        return sessionService.getActiveRefreshTokens(userId)
                .stream()
                .map(tokenMapper::toApiModel)
                .peek(model -> {
                    if (sessionId.equals(model.getSessionId()))
                        model.setCurrent(true);
                })
                .toList();
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        UserDto user = userService.getUserById(userId);
        checkPassword(oldPassword, user.getHashedPassword());

        userService.updateUserPassword(userId, newPassword);
        publisher.publishEvent(new AllUserSessionsInvalidatedEvent(user.getId()));
    }

    @Transactional
    public void setPassword(UUID userId, String password) {
        if (userService.isPasswordSetForUser(userId)) {
            throw new PermissionDeniedException("Password has already been set");
        }
        userService.updateUserPassword(userId, password);
        publisher.publishEvent(new AllUserSessionsInvalidatedEvent(userId));
    }

    private void checkPassword(String enteredPassword, String hashedPassword) {
        if (hashedPassword == null) {
            throw new PasswordNotSetException();
        }
        if (!hasher.verify(enteredPassword, hashedPassword)) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }
}
