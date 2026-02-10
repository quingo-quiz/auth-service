package tech.arhr.quingo.auth_service.services;

import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;

@Service
public class TokenService {
    public TokenDto createAccessToken(UserDto user) {
        return null;
    }

    public TokenDto createRefreshToken(UserDto user) {
        return null;
    }

    public boolean validateToken(String token) {
        return true;
    }

    public UserDto getUserFromToken(String accessToken) {
        return null;
    }

    public void revokeRefreshToken(String refreshToken) {
    }

    public void revokeAllUserTokens(String refreshToken){}
}
