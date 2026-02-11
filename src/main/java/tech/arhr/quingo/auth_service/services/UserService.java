package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.utils.PasswordHasher;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JpaUserRepository userRepository;


    public UserDto checkPassword(String email, String password) {
        return null;
    }

    public UserDto createUser(RegisterRequest request) {
        String hashedPassword = PasswordHasher.hashPassword(request.getPassword());

        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .hashedPassword(hashedPassword)
                .build();
        userRepository.save(userEntity);
        return UserDto.toDto(userEntity);
    }

    public UserDto getUserById(UUID id) {
        return null;
    }
}
