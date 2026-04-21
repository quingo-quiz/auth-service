package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.sql.JpaSocialAccountRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.SocialAccountEntity;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;
import tech.arhr.quingo.auth_service.utils.SocialAccountMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SocialAccountServiceTest {

    @Mock
    private JpaSocialAccountRepository socialAccountRepository;

    @Mock
    private SocialAccountMapper socialAccountMapper;

    private SocialAccountService socialAccountService;

    @BeforeEach
    void setUp() {
        socialAccountService = new SocialAccountService(socialAccountRepository, socialAccountMapper);
    }

    @Test
    void findByProviderAndProviderUserId_NotFound_ThrowsEntityNotFoundException() {
        when(socialAccountRepository.findByProviderEqualsAndProviderUserIdEquals(OAuth2Provider.GITHUB, "provider-id"))
                .thenReturn(List.of());

        assertThatThrownBy(() -> socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GITHUB, "provider-id"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Social account not found");
    }

    @Test
    void findByProviderAndProviderUserId_Found_ReturnsDto() {
        SocialAccountEntity entity = SocialAccountEntity.builder()
                .id(UUID.randomUUID())
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId("provider-id")
                .build();
        SocialAccountDto dto = new SocialAccountDto();
        dto.setId(entity.getId());

        when(socialAccountRepository.findByProviderEqualsAndProviderUserIdEquals(OAuth2Provider.GOOGLE, "provider-id"))
                .thenReturn(List.of(entity));
        when(socialAccountMapper.toDto(entity)).thenReturn(dto);

        SocialAccountDto result = socialAccountService.findByProviderAndProviderUserId(OAuth2Provider.GOOGLE, "provider-id");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    void linkSocialAccount_ValidData_SavesEntity() {
        UUID userId = UUID.randomUUID();
        OAuth2UserData userData = OAuth2UserData.builder()
                .provider(OAuth2Provider.GITHUB)
                .providerUserId("provider-id")
                .build();

        socialAccountService.linkSocialAccount(userData, userId);

        ArgumentCaptor<SocialAccountEntity> captor = ArgumentCaptor.forClass(SocialAccountEntity.class);
        verify(socialAccountRepository).save(captor.capture());

        SocialAccountEntity saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getProvider()).isEqualTo(OAuth2Provider.GITHUB);
        assertThat(saved.getProviderUserId()).isEqualTo("provider-id");
    }
}
