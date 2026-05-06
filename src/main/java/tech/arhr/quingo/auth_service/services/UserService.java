package tech.arhr.quingo.auth_service.services;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.dto.UpdateUserRequest;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import tech.arhr.quingo.auth_service.events.user.UserEmailVerifiedEvent;
import tech.arhr.quingo.auth_service.events.user.UserPasswordResetEvent;
import tech.arhr.quingo.auth_service.events.user.UserRolesChangedEvent;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.auth.PasswordNotSetException;
import tech.arhr.quingo.auth_service.exceptions.auth.UsernameAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {
    private final VerificationService verificationService;
    private final JpaUserRepository userRepository;
    private final UserMapper userMapper;
    private final Hasher hasher;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher publisher;

    public UserService(
            VerificationService verificationService,
            JpaUserRepository userRepository,
            UserMapper userMapper,
            Hasher hasher, CacheManager cacheManager, ApplicationEventPublisher publisher) {
        this.verificationService = verificationService;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.hasher = hasher;
        this.cacheManager = cacheManager;
        this.publisher = publisher;
    }


    @Transactional(readOnly = true)
    public UserDto checkPasswordReturnUser(String email, String password) {
        UserEntity userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (userEntity.getHashedPassword() == null) {
            throw new PasswordNotSetException();
        }
        if (!hasher.verify(password, userEntity.getHashedPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return userMapper.toDto(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto checkPasswordReturnUser(UUID userId, String password) {
        UserEntity userEntity = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid userId or password"));


        if (userEntity.getHashedPassword() == null) {
            throw new PasswordNotSetException();
        }

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
    @CacheEvict(value = "users:cached", key = "#userId")
    public void updateUserPassword(UUID userId, String password) {
        UserEntity entity = findByIdOrThrow(userId);
        entity.setHashedPassword(hasher.hash(password));
        userRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "users:cached", key = "#userId")
    public void clearUserPassword(UUID userId) {
        UserEntity entity = findByIdOrThrow(userId);
        entity.setHashedPassword(null);
        userRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "users:cached", key = "#userId")
    public void blockUser(UUID userId) {
        UserEntity entity = findByIdOrThrow(userId);

        entity.setAccountStatus(AccountStatus.BLOCKED);
        userRepository.save(entity);

        publisher.publishEvent(new AllUserSessionsInvalidatedEvent(userId));
    }

    @Transactional
    @CacheEvict(value = "users:cached", key = "#userId")
    public void unblockUser(UUID userId) {
        UserEntity entity = findByIdOrThrow(userId);

        entity.setAccountStatus(AccountStatus.ACTIVE);
        userRepository.save(entity);
    }

    @Transactional
    @CacheEvict(value = "users:cached", key = "#userId")
    public void changeUserRoles(UUID userId, List<UserRole> userRoles) {
        UserEntity entity = findByIdOrThrow(userId);

        entity.setRoles(userRoles);
        userRepository.save(entity);

        publisher.publishEvent(new UserRolesChangedEvent(userId, userRoles));
    }

    @Transactional(readOnly = true)
    public boolean isPasswordSetForUser(UUID userId) {
        UserEntity entity = findByIdOrThrow(userId);
        return entity.getHashedPassword() != null;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "users:cached", key = "#id")
    public UserDto getUserById(UUID id) {
        UserEntity userEntity = findByIdOrThrow(id);

        return userMapper.toDto(userEntity);
    }

    @Transactional(readOnly = true)
    public UserDto getUserByEmail(String email) {
        UserEntity userEntity = findByEmailOrThrow(email);

        return userMapper.toDto(userEntity);
    }

    @Transactional
    public void sendResetPassword(String email) {
        Optional<UserEntity> opt = userRepository.findByEmail(email);
        if (opt.isPresent()) {
            verificationService.sendResetPasswordEmail(email, opt.get().getId());
        }
    }

    @Transactional
    @CacheEvict(value = "users:cached", key = "#userId")
    public UserDto updateUser(UUID userId, UpdateUserRequest request) {
        UserEntity userEntity = findByIdOrThrow(userId);

        if (request.getUsername() != null && !request.getUsername().equals(userEntity.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UsernameAlreadyExistsException();
            }
            userEntity.setUsername(request.getUsername());
        }

        if (request.getBio() != null) {
            userEntity.setBio(request.getBio());
        }

        userEntity = userRepository.save(userEntity);
        return userMapper.toDto(userEntity);
    }

    @EventListener(UserEmailVerifiedEvent.class)
    public void onUserEmailVerified(UserEmailVerifiedEvent event) {
        UUID userId = event.userId();

        UserEntity userEntity = findByIdOrThrow(userId);
        userEntity.setEmailVerified(true);
        evictUserCache(userId);
        userRepository.save(userEntity);
    }

    @EventListener(UserPasswordResetEvent.class)
    public void onUserPasswordReset(UserPasswordResetEvent event) {
        UUID userId = event.userId();
        updateUserPassword(userId, event.newPassword());
        evictUserCache(userId);

        publisher.publishEvent(new AllUserSessionsInvalidatedEvent(userId));
    }

    private UserEntity findByIdOrThrow(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with id: " + id));
    }

    private UserEntity findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found with email: " + email));
    }

    private void evictUserCache(UUID userId) {
        var cache = cacheManager.getCache("users:cached");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
