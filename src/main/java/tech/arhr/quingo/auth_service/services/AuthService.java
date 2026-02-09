package tech.arhr.quingo.auth_service.services;

import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.api.rest.dto.RegisterRequest;
import tech.arhr.quingo.auth_service.dto.TokenPairDto;

@Service
public class AuthService {
    public TokenPairDto register(RegisterRequest registerRequest) {
        return null;
    }
}
