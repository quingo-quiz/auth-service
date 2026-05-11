package tech.arhr.quingo.auth_service.services.oauth2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import org.mockito.ArgumentCaptor;
import tech.arhr.quingo.auth_service.services.SocialAccountService;
import tech.arhr.quingo.auth_service.services.UserService;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2IntegrationServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private SocialAccountService socialAccountService;

    @Mock
    private ApplicationEventPublisher publisher;

    private OAuth2IntegrationService integrationService;

    @BeforeEach
    void setUp() {
        integrationService = new OAuth2IntegrationService(userService, socialAccountService, publisher);
    }

    @Test
    void processOAuth2User_ExistingSocialAccount_ReturnsLinkedUser() {
        OAuth2UserData userData = OAuth2UserData.builder()
                .provider(OAuth2Provider.GITHUB)
                .providerUserId("provider-id")
                .email("user@test.com")
                .emailVerified(true)
                .username("username")
                .build();

        UUID userId = UUID.randomUUID();
        SocialAccountDto account = new SocialAccountDto();
        account.setUserId(userId);
        account.setProvider(OAuth2Provider.GITHUB);
        account.setProviderUserId("provider-id");
        UserDto user = UserDto.builder().id(userId).build();

        when(socialAccountService.findByProviderAndProviderUserIdOptional(OAuth2Provider.GITHUB, "provider-id"))
                .thenReturn(Optional.of(account));
        when(userService.getUserById(userId)).thenReturn(user);

        UserDto result = integrationService.processOAuth2User(userData);

        assertThat(result).isEqualTo(user);
        verify(userService, never()).createUserFromOAuth2(any());
        verify(socialAccountService, never()).linkSocialAccount(any(), any());
    }

    @Test
    void processOAuth2User_NewProviderUser_ExistingLocalUnverifiedUser_ClearsPasswordAndPublishesEvent() {
        OAuth2UserData userData = OAuth2UserData.builder()
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId("google-id")
                .email("user@test.com")
                .emailVerified(true)
                .username("username")
                .build();

        UserDto localUser = UserDto.builder()
                .id(UUID.randomUUID())
                .email("user@test.com")
                .emailVerified(false)
                .build();

        when(socialAccountService.findByProviderAndProviderUserIdOptional(OAuth2Provider.GOOGLE, "google-id"))
                .thenReturn(Optional.empty());
        when(userService.getUserByEmailOptional("user@test.com")).thenReturn(Optional.of(localUser));

        UserDto result = integrationService.processOAuth2User(userData);

        assertThat(result).isEqualTo(localUser);
        verify(userService).clearUserPassword(localUser.getId());
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(publisher).publishEvent(captor.capture());
        assertThat(((AllUserSessionsInvalidatedEvent) captor.getValue()).userId()).isEqualTo(localUser.getId());
        verify(socialAccountService).linkSocialAccount(userData, localUser.getId());
    }

    @Test
    void processOAuth2User_NewProviderUser_NewLocalUser_CreatesAndLinks() {
        OAuth2UserData userData = OAuth2UserData.builder()
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId("google-id")
                .email("new@test.com")
                .emailVerified(true)
                .username("new-user")
                .build();

        UserDto created = UserDto.builder().id(UUID.randomUUID()).build();

        when(socialAccountService.findByProviderAndProviderUserIdOptional(OAuth2Provider.GOOGLE, "google-id"))
                .thenReturn(Optional.empty());
        when(userService.getUserByEmailOptional("new@test.com"))
                .thenReturn(Optional.empty());
        when(userService.createUserFromOAuth2(userData)).thenReturn(created);

        UserDto result = integrationService.processOAuth2User(userData);

        assertThat(result).isEqualTo(created);
        verify(socialAccountService).linkSocialAccount(userData, created.getId());
    }

    @Test
    void processOAuth2User_UnverifiedProviderEmailForExistingUser_ThrowsOAuth2AuthenticationException() {
        OAuth2UserData userData = OAuth2UserData.builder()
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId("google-id")
                .email("user@test.com")
                .emailVerified(false)
                .username("username")
                .build();

        UserDto localUser = UserDto.builder().id(UUID.randomUUID()).emailVerified(true).build();

        when(socialAccountService.findByProviderAndProviderUserIdOptional(OAuth2Provider.GOOGLE, "google-id"))
                .thenReturn(Optional.empty());
        when(userService.getUserByEmailOptional("user@test.com")).thenReturn(Optional.of(localUser));

        assertThatThrownBy(() -> integrationService.processOAuth2User(userData))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("unverified provider account email");

        verify(socialAccountService, never()).linkSocialAccount(any(), any());
    }
}
