package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.ServiceException;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.utils.PasswordHasher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JpaUserRepository userRepository;


    public UserDto checkPassword(String email, String password) {
        String hashedPassword = PasswordHasher.hashPassword(password);
        List<UserEntity> entities = userRepository.findByEmailEqualsAndHashedPasswordEquals(email, hashedPassword);
        if (entities.isEmpty()) {
            throw new InvalidCredentialsException("Invalid email or password");
        } else {
            return UserDto.toDto(entities.getFirst());
        }
    }

    public UserDto createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

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
        Optional<UserEntity> optional = userRepository.findById(id);
        if (optional.isPresent()) {
            return UserDto.toDto(optional.get());
        } else {
            throw new EntityNotFoundException("User not found");
        }
    }
}
