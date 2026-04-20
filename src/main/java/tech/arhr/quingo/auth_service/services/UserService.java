package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.OAuth2UserData;
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
public class UserService {
    private final TokenService tokenService;
    private final JpaUserRepository userRepository;
    private final UserMapper userMapper;
    private final Hasher hasher;

    public UserService(
            @Lazy TokenService tokenService,
            JpaUserRepository userRepository,
            UserMapper userMapper,
            Hasher hasher) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.hasher = hasher;
    }


    @Transactional(readOnly = true)
    public UserDto checkPasswordReturnUser(String email, String password) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!hasher.verify(password, userEntity.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return userMapper.toDto(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto checkPasswordReturnUser(UUID userId, String password) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid userId or password"));

        if (!hasher.verify(password, userEntity.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid userId or password");
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

    @Transactional
    public UserDto createUserFromOAuth2(OAuth2UserData userData) {
        if (userRepository.existsByEmail(userData.getEmail())) {
            throw new EmailAlreadyExistsException();
        }

        if (userRepository.existsByUsername(userData.getUsername())) {
            throw new UsernameAlreadyExistsException();
        }

        UserEntity userEntity = UserEntity.builder()
                .username(userData.getUsername())
                .email(userData.getEmail())
                .hashedPassword(null)
                .roles(List.of(UserRole.USER))
                .emailVerified(userData.isEmailVerified())
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        userRepository.save(userEntity);
        return userMapper.toDto(userEntity);
    }

    @Transactional
    public void updateUserPassword(UUID userId, String password) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        entity.setHashedPassword(hasher.hash(password));
        userRepository.save(entity);
    }

    @Transactional
    public void clearUserPassword(UUID userId) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));
        entity.setHashedPassword(null);
        userRepository.save(entity);
    }

    @Transactional
    public void blockUser(UUID userId) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        entity.setAccountStatus(AccountStatus.BLOCKED);
        userRepository.save(entity);
        tokenService.revokeAllUserTokens(userId);
    }

    @Transactional
    public void unblockUser(UUID userId) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        entity.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(entity);
    }

    @Transactional
    public void changeUserRoles(UUID userId, List<UserRole> userRoles) {
        UserEntity entity = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User with id " + userId + " not found"));

        entity.setRoles(userRoles);
        userRepository.save(entity);
        tokenService.refreshSessions(userId);
    }

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID id) {
        UserEntity userEntity = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userMapper.toDto(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return userMapper.toDto(userEntity);
    }
}
