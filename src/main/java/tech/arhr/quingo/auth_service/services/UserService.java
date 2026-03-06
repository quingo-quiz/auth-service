package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.auth.UsernameAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.UserMapper;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final JpaUserRepository userRepository;
    private final UserMapper userMapper;
    private final Hasher hasher;


    @Transactional(readOnly = true)
    public UserDto checkPasswordReturnUser(String email, String password) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!hasher.verify(password, userEntity.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return userMapper.toDto(userEntity);
    }

    @Transactional
    public UserDto createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        String hashedPassword = hasher.hash(request.getPassword());
        UserEntity userEntity = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .hashedPassword(hashedPassword)
                .roles(List.of(UserRole.USER))
                .emailVerified(false)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userRepository.save(userEntity);
        return userMapper.toDto(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userMapper.toDto(userEntity);
    }
}
