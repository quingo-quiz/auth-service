package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.sql.entity.SocialAccountEntity;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaSocialAccountRepository extends JpaRepository<SocialAccountEntity, UUID> {

    List<SocialAccountEntity> findByProviderEqualsAndProviderUserIdEquals(OAuth2Provider provider, String providerUserId);
}
