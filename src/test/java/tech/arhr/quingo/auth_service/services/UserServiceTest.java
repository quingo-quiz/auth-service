package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidCredentialsException;
import tech.arhr.quingo.auth_service.exceptions.auth.UsernameAlreadyExistsException;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.UserMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private TokenService tokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private Hasher hasher;

    @Mock
    private JpaUserRepository jpaUserRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void checkPasswordReturnUser_EmailNotFound_ThrowsInvalidCredentialsException() {
        String email = "email@example.com";
        String password = "password";

        when(jpaUserRepository.findByEmail(email)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.checkPasswordReturnUser(email, password))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void checkPasswordReturnUser_InvalidPassword_ThrowsInvalidCredentialsException() {
        String email = "email@example.com";
        String password = "wrong-password";
        String hashedPassword = "hashed-correctPassword";

        UserEntity userEntity = new UserEntity();
        userEntity.setEmail(email);
        userEntity.setHashedPassword(hashedPassword);

        when(jpaUserRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(hasher.verify(password, hashedPassword)).thenReturn(false);

        assertThatThrownBy(() -> userService.checkPasswordReturnUser(email, password))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void checkPasswordReturnUser_ValidCredentials_ReturnsUserDto() {
        String email = "email@example.com";
        String password = "correctPassword";
        String hashedPassword = "hashed-correctPassword";

        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID());
        userEntity.setEmail(email);
        userEntity.setHashedPassword(hashedPassword);

        UserDto expectedDto = new UserDto();
        expectedDto.setId(userEntity.getId());
        expectedDto.setEmail(email);

        when(jpaUserRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
        when(hasher.verify(password, hashedPassword)).thenReturn(true);
        when(userMapper.toDto(userEntity)).thenReturn(expectedDto);

        UserDto result = userService.checkPasswordReturnUser(email, password);

        assertThat(result)
                .isNotNull()
                .isEqualTo(expectedDto);
    }

    @Test
    void getUserById_UserNotFound_ThrowsEntityNotFoundException() {
        UUID userId = UUID.randomUUID();

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(userId))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getUserById_UserExists_ReturnsUserDto() {
        UUID userId = UUID.randomUUID();

        UserEntity userEntity = new UserEntity();
        userEntity.setId(userId);

        UserDto expectedDto = new UserDto();
        expectedDto.setId(userId);

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toDto(userEntity)).thenReturn(expectedDto);

        UserDto result = userService.getUserById(userId);

        assertThat(result)
                .isNotNull()
                .isEqualTo(expectedDto);
    }


    @Test
    void createUser_EmailAlreadyExists_ThrowsEmailAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("email@example.com");
        request.setUsername("user");

        when(jpaUserRepository.existsByEmail(request.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void createUser_UsernameAlreadyExists_ThrowsUsernameAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("email@example.com");
        request.setUsername("takenUsername");

        when(jpaUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(jpaUserRepository.existsByUsername(request.getUsername())).thenReturn(true);


        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(jpaUserRepository, never()).save(any());
    }

    @Test
    void createUser_EmailAndUsernameNotExists_ReturnsUserDto() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("email@example.com");
        request.setUsername("user");
        request.setPassword("rawPassword");
        String hashedPassword = "hashed-rawPassword";

        UserEntity savedEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .email(request.getEmail())
                .username(request.getUsername())
                .hashedPassword(hashedPassword)
                .roles(List.of(UserRole.USER))
                .emailVerified(false)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        UserDto expectedDto = new UserDto();
        expectedDto.setId(savedEntity.getId());
        expectedDto.setEmail(savedEntity.getEmail());

        when(jpaUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(jpaUserRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(hasher.hash(request.getPassword())).thenReturn(hashedPassword);
        when(jpaUserRepository.save(any(UserEntity.class))).thenReturn(savedEntity);
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(expectedDto);


        UserDto result = userService.createUser(request);


        assertThat(result)
                .isNotNull()
                .isEqualTo(expectedDto);
    }

    @Test
    void createUser_EmailAndUsernameNotExists_SavesEntityWithCorrectFields() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("email@example.com");
        request.setUsername("user");
        request.setPassword("rawPassword");
        String hashedPassword = "hashed-rawPassword";

        when(jpaUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(jpaUserRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(hasher.hash(request.getPassword())).thenReturn(hashedPassword);
        when(jpaUserRepository.save(any(UserEntity.class))).thenAnswer(i -> i.getArgument(0));
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(new UserDto());


        userService.createUser(request);


        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jpaUserRepository).save(captor.capture());

        UserEntity saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("email@example.com");
        assertThat(saved.getUsername()).isEqualTo("user");
        assertThat(saved.getHashedPassword()).isEqualTo(hashedPassword);
        assertThat(saved.getRoles()).containsExactly(UserRole.USER);
        assertThat(saved.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(saved.isEmailVerified()).isFalse();
    }

    @Test
    void checkPasswordReturnUserById_ValidCredentials_ReturnsUserDto() {
        UUID userId = UUID.randomUUID();
        String rawPassword = "secret";
        String hashedPassword = "hashed-secret";

        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setHashedPassword(hashedPassword);

        UserDto expected = UserDto.builder().id(userId).build();

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(hasher.verify(rawPassword, hashedPassword)).thenReturn(true);
        when(userMapper.toDto(entity)).thenReturn(expected);

        UserDto result = userService.checkPasswordReturnUser(userId, rawPassword);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void updateUserPassword_UserExists_SavesHashedPassword() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));
        when(hasher.hash("newPassword")).thenReturn("hashed-newPassword");

        userService.updateUserPassword(userId, "newPassword");

        assertThat(entity.getHashedPassword()).isEqualTo("hashed-newPassword");
        verify(jpaUserRepository).save(entity);
    }

    @Test
    void clearUserPassword_UserExists_SetsPasswordToNull() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setHashedPassword("hashed");

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));

        userService.clearUserPassword(userId);

        assertThat(entity.getHashedPassword()).isNull();
        verify(jpaUserRepository).save(entity);
    }

    @Test
    void blockUser_UserExists_SetsBlockedAndRevokesSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setAccountStatus(AccountStatus.ACTIVE);

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));

        userService.blockUser(userId);

        assertThat(entity.getAccountStatus()).isEqualTo(AccountStatus.BLOCKED);
        verify(jpaUserRepository).save(entity);
        verify(tokenService).revokeAllUserTokens(userId);
    }

    @Test
    void unblockUser_UserExists_SetsActive() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setAccountStatus(AccountStatus.BLOCKED);

        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));

        userService.unblockUser(userId);

        assertThat(entity.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(jpaUserRepository).save(entity);
    }

    @Test
    void changeUserRoles_UserExists_UpdatesRolesAndRefreshesSessions() {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);

        List<UserRole> newRoles = List.of(UserRole.ADMIN, UserRole.USER);
        when(jpaUserRepository.findById(userId)).thenReturn(Optional.of(entity));

        userService.changeUserRoles(userId, newRoles);

        assertThat(entity.getRoles()).containsExactly(UserRole.ADMIN, UserRole.USER);
        verify(jpaUserRepository).save(entity);
        verify(tokenService).refreshSessions(userId);
    }

    @Test
    void createUserFromOAuth2_ValidData_SavesAndReturnsDto() {
        OAuth2UserData userData = OAuth2UserData.builder()
                .email("oauth@test.com")
                .username("oauth-user")
                .emailVerified(true)
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId("provider-id")
                .build();

        UserDto expected = UserDto.builder().id(UUID.randomUUID()).email("oauth@test.com").build();

        when(jpaUserRepository.existsByEmail(userData.getEmail())).thenReturn(false);
        when(jpaUserRepository.existsByUsername(userData.getUsername())).thenReturn(false);
        when(jpaUserRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toDto(any(UserEntity.class))).thenReturn(expected);

        UserDto result = userService.createUserFromOAuth2(userData);

        assertThat(result).isEqualTo(expected);
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(jpaUserRepository).save(captor.capture());
        UserEntity saved = captor.getValue();

        assertThat(saved.getEmail()).isEqualTo("oauth@test.com");
        assertThat(saved.getUsername()).isEqualTo("oauth-user");
        assertThat(saved.getHashedPassword()).isNull();
        assertThat(saved.isEmailVerified()).isTrue();
        assertThat(saved.getRoles()).containsExactly(UserRole.USER);
    }
}