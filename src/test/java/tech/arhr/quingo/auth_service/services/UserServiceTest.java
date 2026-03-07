package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
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
}