package tech.arhr.quingo.auth_service.providers;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.services.TokenService;
import tech.arhr.quingo.auth_service.services.UserService;

@Component
@RequiredArgsConstructor
public class LocalAuthProvider implements AuthProvider {
    private final TokenService tokenService;
    private final UserService userService;

    @Override
    public AuthProviderType getProviderType() {
        return AuthProviderType.LOCAL;
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        UserDto user = userService.checkPassword(request.getEmail(), request.getPassword());

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        UserDto user = userService.createUser(request);

        return AuthResponse.builder()
                .accessToken(tokenService.createAccessToken(user))
                .refreshToken(tokenService.createRefreshToken(user))
                .build();
    }
}
