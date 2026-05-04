package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.JpaSocialAccountRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.SocialAccountEntity;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;
import tech.arhr.quingo.auth_service.utils.SocialAccountMapper;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private final JpaSocialAccountRepository socialAccountRepository;
    private final SocialAccountMapper socialAccountMapper;

    public SocialAccountDto findByProviderAndProviderUserId(OAuth2Provider provider, String providerUserId) {
        List<SocialAccountEntity> entities = socialAccountRepository.findByProviderEqualsAndProviderUserIdEquals(provider, providerUserId);
        if (entities.isEmpty()) {throw new EntityNotFoundException("Social account not found");}

        return socialAccountMapper.toDto(entities.getFirst());
    }

    @Transactional
    public void linkSocialAccount(OAuth2UserData userData, UUID userId){
        SocialAccountEntity entity = SocialAccountEntity.builder()
                .userId(userId)
                .providerUserId(userData.getProviderUserId())
                .provider(userData.getProvider())
                .build();
        socialAccountRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<OAuth2Provider> getUserLinkedProviders(UUID userId){
        return socialAccountRepository.findByUserId(userId).stream()
                .map(SocialAccountEntity::getProvider)
                .collect(Collectors.toList());
    }
}
