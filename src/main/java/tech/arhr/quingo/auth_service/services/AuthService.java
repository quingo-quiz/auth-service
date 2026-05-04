package tech.arhr.quingo.auth_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.api.rest.models.ChangePasswordRequest;
import tech.arhr.quingo.auth_service.api.rest.models.TokenModel;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.exceptions.auth.AccountNotActiveException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class AuthService {
    private final TokenService tokenService;
    private final UserService userService;
    private final VerificationService verificationService;
    private final MfaService mfaService;
    private final TokenMapper tokenMapper;
    private final SocialAccountService socialAccountService;

    public AuthService(
            TokenService tokenService,
            TokenMapper tokenMapper,
            UserService userService,
            VerificationService verificationService,
            SocialAccountService socialAccountService,
            MfaService mfaService
    ) {
        this.tokenService = tokenService;
        this.userService = userService;
        this.tokenMapper = tokenMapper;
        this.verificationService = verificationService;
        this.mfaService = mfaService;
        this.socialAccountService = socialAccountService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserDto user = userService.createUser(request);
        verificationService.sendVerificationToken(user);

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        UserDto user = userService.checkPasswordReturnUser(request.getEmail(), request.getPassword());

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Account status is " + user.getAccountStatus());
        }

        if (user.isMfaEnabled()) {
            return AuthResponse.builder()
                    .mfaTempToken(tokenService.createMfaTempToken(user))
                    .mfaRequired(true)
                    .build();
        }

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    @Transactional
    public AuthResponse verifyOtpIssueTokens(OtpVerifyRequest request) {
        UUID userId = tokenService.validateMfaTempToken(request.getMfaTempToken());
        mfaService.verifyOtpCode(userId, request);

        UserDto user = userService.getUserById(userId);

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        UserDto user = tokenService.getUserFromTokenWithQuery(refreshToken);
        tokenService.revokeRefreshToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Transactional
    public void logout(String refreshToken, String accessToken) {
        tokenService.revokeRefreshToken(refreshToken);
        if (accessToken != null) {
            tokenService.blockAccessToken(accessToken);
        }
    }

    @Transactional
    public void logoutTokenById(String refreshToken, UUID tokenId) {
        tokenService.revokeRefreshTokenById(refreshToken, tokenId);
    }

    @Transactional
    public void logoutAll(String refreshToken) {
        tokenService.revokeAllUserTokens(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserDto authorize(String accessToken) {
        tokenService.validateAccessToken(accessToken);
        return tokenService.getUserFromTokenNoQuery(accessToken);
    }

    @Transactional(readOnly = true)
    public List<TokenModel> getActiveRefreshTokens(UUID userId) {
        return tokenService.getActiveRefreshTokens(userId)
                .stream()
                .map(tokenMapper::toApiModel)
                .toList();
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        userService.checkPasswordReturnUser(userId, oldPassword);
        userService.updateUserPassword(userId, newPassword);
        tokenService.revokeAllUserTokens(userId);
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
                tokenService.revokeAllUserTokens(user.getId());
            }
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        } catch (EntityNotFoundException e1) {
            UserDto user = userService.createUserFromOAuth2(userData);
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        }
    }
}
