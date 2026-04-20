package tech.arhr.quingo.auth_service.dto.oauth2;

import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

import java.util.Map;

@Component
public class OAuth2UserDataParser {
    public OAuth2UserData parseUserData(OAuth2Provider provider, OAuth2User user) {
        switch (provider) {
            case GOOGLE -> {
                return handleGoogle(user);
            }
            case GITHUB -> {
                return handleGitHub(user);
            }
        }
        return null;
    }

    private OAuth2UserData handleGoogle(OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        String email = (String) attributes.get("email");
        boolean emailVerified = (boolean) attributes.get("email_verified");
        String userName = (String) attributes.get("name");
        String providerUserId = attributes.get("sub").toString();

        return OAuth2UserData.builder()
                .email(email)
                .emailVerified(emailVerified)
                .username(userName)
                .provider(OAuth2Provider.GOOGLE)
                .providerUserId(providerUserId)
                .build();
    }

    private OAuth2UserData handleGitHub(OAuth2User user) {
        Map<String, Object> attributes = user.getAttributes();
        String email = (String) attributes.get("email");
        String userName = (String) attributes.get("name");
        String providerUserId = attributes.get("id").toString();

        return OAuth2UserData.builder()
                .email(email)
                .emailVerified(false)
                .username(userName)
                .provider(OAuth2Provider.GITHUB)
                .providerUserId(providerUserId)
                .build();
    }
}
