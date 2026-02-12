package tech.arhr.quingo.auth_service.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.utils.Hasher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {
    private final JpaUserRepository userRepository;


    public UserDto checkPassword(String email, String password) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!Hasher.verify(password, userEntity.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return UserDto.toDto(userEntity);
    }

    public UserDto createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        String hashedPassword = Hasher.hash(request.getPassword());
        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .hashedPassword(hashedPassword)
                .isEmailVerified(false)
                .isAccountBlocked(false)
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
