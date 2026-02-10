package tech.arhr.quingo.auth_service.providers;

import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;

public interface AuthProvider {
    AuthProviderType getProviderType();
    AuthResponse authenticate(AuthRequest request);
    AuthResponse register(RegisterRequest request);
}
