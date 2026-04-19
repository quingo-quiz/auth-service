package tech.arhr.quingo.auth_service.services.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.dto.OAuth2UserData;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;
import tech.arhr.quingo.auth_service.services.AuthService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final AuthService authService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String email = (String) attributes.get("email");
        boolean emailVerified = (boolean) attributes.get("email_verified");
        String userName = (String) attributes.get("name");
        String providerUserId = (String) attributes.get("sub");
        String provider = userRequest.getClientRegistration().getRegistrationId();

        OAuth2UserData data = OAuth2UserData.builder()
                .email(email)
                .emailVerified(emailVerified)
                .username(userName)
                .provider(OAuth2Provider.valueOf(provider.toUpperCase()))
                .providerUserId(providerUserId)
                .build();

        if (!emailVerified) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("none"),
                    "You can't auth with unverified " + provider + " account email. Please use password authentication.");
        }

        authService.processOAuth2User(data);
        return oAuth2User;
    }
}
