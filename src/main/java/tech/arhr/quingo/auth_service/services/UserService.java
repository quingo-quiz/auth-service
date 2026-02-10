package tech.arhr.quingo.auth_service.services;

import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;

@Service
public class UserService {
    public UserDto checkPassword(String email, String password) {
        return null;
    }

    public UserDto createUser(RegisterRequest request) {
        return null;
    }
}
