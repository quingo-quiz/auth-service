package tech.arhr.quingo.auth_service.services;

import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.api.rest.dto.AuthRequest;
import tech.arhr.quingo.auth_service.api.rest.dto.RegisterRequest;
import tech.arhr.quingo.auth_service.dto.TokenPairDto;

@Service
public class AuthService {
    public TokenPairDto register(RegisterRequest registerRequest) {
        return null;
    }

    public TokenPairDto authenticate(AuthRequest authRequest) {
        return null;
    }

    public TokenPairDto refresh(String refreshToken) {
        return null;
    }

    public void logout(String token) {
    }

    public void logoutAll(String token) {}
}
