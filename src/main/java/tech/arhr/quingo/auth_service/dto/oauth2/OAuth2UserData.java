package tech.arhr.quingo.auth_service.dto.oauth2;

import lombok.Builder;
import lombok.Data;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

@Data
@Builder
public class OAuth2UserData {
    private String email;
    private boolean emailVerified;
    private String username;
    private String providerUserId;
    private OAuth2Provider provider;
}
