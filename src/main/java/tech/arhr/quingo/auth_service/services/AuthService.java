package tech.arhr.quingo.auth_service.services;

import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.api.rest.dto.AuthRequest;
import tech.arhr.quingo.auth_service.api.rest.dto.RegisterRequest;
import tech.arhr.quingo.auth_service.dto.TokenPairDto;
import tech.arhr.quingo.auth_service.dto.UserDto;

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

    public UserDto authorize(String token) {
        return null;
    }
}
