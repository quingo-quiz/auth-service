package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.ServiceException;
import tech.arhr.quingo.auth_service.providers.AuthProvider;
import tech.arhr.quingo.auth_service.providers.AuthProviderType;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final TokenService tokenService;
    private final List<AuthProvider> authProviders;

    public AuthResponse register(RegisterRequest registerRequest) {
        AuthProvider provider = getAuthProvider(registerRequest.getProvider());
        return provider.register(registerRequest);
    }

    public AuthResponse authenticate(AuthRequest authRequest) {
        AuthProvider provider = getAuthProvider(authRequest.getProvider());
        return provider.authenticate(authRequest);
    }

    public AuthResponse refresh(String refreshToken) {

        UserDto user = tokenService.getUserFromToken(refreshToken);
        tokenService.revokeRefreshToken(refreshToken);

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    public void logoutAll(String refreshToken) {
        tokenService.revokeAllUserTokens(refreshToken);
    }

    public UserDto authorize(String token) {
        return tokenService.getUserFromToken(token);
    }

    private AuthProvider getAuthProvider(String authProviderType) {
        AuthProviderType type = AuthProviderType.valueOf(authProviderType);

        return authProviders.stream()
                .filter((provider) -> {
                    return provider.getProviderType().equals(type);
                })
                .findFirst()
                .orElseThrow(() -> new ServiceException("Auth Provider Type Not Found"));
    }
}
