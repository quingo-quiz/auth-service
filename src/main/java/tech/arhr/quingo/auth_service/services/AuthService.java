package tech.arhr.quingo.auth_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.api.rest.models.SessionModel;
import tech.arhr.quingo.auth_service.api.security.CustomWebAuthenticationDetails;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.*;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
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
    private final VerificationService verificationService;
    private final MfaService mfaService;
    private final TokenMapper tokenMapper;
    private final SocialAccountService socialAccountService;
    private final JwtProvider jwtProvider;
    private final Hasher hasher;

    public AuthService(
            SessionService sessionService,
            TokenMapper tokenMapper,
            UserService userService,
            VerificationService verificationService,
            SocialAccountService socialAccountService,
            MfaService mfaService,
            Hasher hasher,
            JwtProvider jwtProvider
    ) {
        this.sessionService = sessionService;
        this.userService = userService;
        this.tokenMapper = tokenMapper;
        this.verificationService = verificationService;
        this.mfaService = mfaService;
        this.socialAccountService = socialAccountService;
        this.jwtProvider = jwtProvider;
        this.hasher = hasher;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserDto user = userService.createUser(request);
        verificationService.sendVerificationEmail(user);

        SessionTokens tokens = sessionService.createSession(user, getClientInfoFromContext());
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
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

            SessionTokens tokens = sessionService.createSession(user, getClientInfoFromContext());
            return AuthResponse.builder()
                    .accessToken(tokens.getAccessToken())
                    .refreshToken(tokens.getRefreshToken())
                    .build();

        } catch (EntityNotFoundException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    @Transactional
    public AuthResponse verifyOtpIssueTokens(OtpVerifyRequest request) {
        UUID userId = jwtProvider.validateMfaTempToken(request.getMfaTempToken());
        mfaService.verifyOtpCode(userId, request.getCode());

        UserDto user = userService.getUserById(userId);

        SessionTokens tokens = sessionService.createSession(user, getClientInfoFromContext());
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(tokens.getRefreshToken())
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        sessionService.validateRefreshToken(refreshToken);
        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        UserDto user = userService.getUserById(userId);

        sessionService.revokeRefreshToken(refreshToken);

        SessionTokens tokens = sessionService.createSession(user, getClientInfoFromContext());
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
    public List<SessionModel> getActiveRefreshTokens(UUID userId) {
        return sessionService.getActiveRefreshTokens(userId)
                .stream()
                .map(tokenMapper::toApiModel)
                .toList();
    }

    @Transactional
    public void changePassword(UUID userId, String oldPassword, String newPassword) {
        UserDto user = userService.getUserById(userId);
        checkPassword(oldPassword, user.getHashedPassword());

        userService.updateUserPassword(userId, newPassword);
        sessionService.revokeAllUserTokens(userId);
    }

    @Transactional
    public void setPassword(UUID userId, String password) {
        if (userService.isPasswordSetForUser(userId)) {
            throw new PermissionDeniedException("Password has already been set");
        }
        userService.updateUserPassword(userId, password);
        sessionService.revokeAllUserTokens(userId);
    }

    @Transactional
    public UserDto processOAuth2User(OAuth2UserData userData) {
        try {
            SocialAccountDto account = socialAccountService.findByProviderAndProviderUserId(
                    userData.getProvider(),
                    userData.getProviderUserId());
            return userService.getUserById(account.getUserId());
        } catch (EntityNotFoundException e) {
            return handleNewSocialAccountLink(userData);
        }
    }

    @Transactional
    protected UserDto handleNewSocialAccountLink(OAuth2UserData userData) {
        try {
            UserDto user = userService.getUserByEmail(userData.getEmail());
            if (!userData.isEmailVerified()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("none"),
                        "We can't merge your account with unverified provider account email. Please use password authentication.");
            }
            if (!user.isEmailVerified()) {
                userService.clearUserPassword(user.getId());
                sessionService.revokeAllUserTokens(user.getId());
            }
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        } catch (EntityNotFoundException e1) {
            UserDto user = userService.createUserFromOAuth2(userData);
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        }
    }

    private void checkPassword(String enteredPassword, String hashedPassword) {
        if (hashedPassword == null) {
            throw new PasswordNotSetException();
        }
        if (!hasher.verify(enteredPassword, hashedPassword)) {
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    private UserAgentInfoDto getClientInfoFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof CustomWebAuthenticationDetails details) {
            return details.getUserAgentInfo();
        }
        return new UserAgentInfoDto();
    }
}
