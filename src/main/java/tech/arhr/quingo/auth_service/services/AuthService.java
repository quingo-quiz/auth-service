package tech.arhr.quingo.auth_service.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;
import tech.arhr.quingo.auth_service.providers.AuthProvider;
import tech.arhr.quingo.auth_service.providers.AuthProviderType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AuthService {
    private final TokenService tokenService;
    private final List<AuthProvider> authProviders;
    private Map<AuthProviderType, AuthProvider> authProviderMap;

    public AuthService(TokenService tokenService,
                       List<AuthProvider> authProviders) {
        this.tokenService = tokenService;
        this.authProviders = authProviders;

        authProviderMap = new HashMap<>();

        authProviders.forEach(authProvider ->
                authProviderMap.put(authProvider.getProviderType(), authProvider)
        );
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        AuthProvider provider = getAuthProvider(registerRequest.getProvider());
        return provider.register(registerRequest);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest authRequest) {
        AuthProvider provider = getAuthProvider(authRequest.getProvider());
        return provider.authenticate(authRequest);
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
    public void logoutAll(String refreshToken) {
        tokenService.revokeAllUserTokens(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserDto authorize(String accessToken) {
        tokenService.validateAccessToken(accessToken);
        return tokenService.getUserFromTokenNoQuery(accessToken);
    }

    private AuthProvider getAuthProvider(String authProviderType) {
        try {
            AuthProviderType type = AuthProviderType.valueOf(authProviderType.toUpperCase());
            AuthProvider authProvider = authProviderMap.get(type);

            if (authProvider == null)
                throw new AuthException("Auth provider not supported");
            return authProvider;
        } catch (IllegalArgumentException e) {
            throw new AuthException("Invalid Auth Provider Type: " + authProviderType);
        }
    }
}
