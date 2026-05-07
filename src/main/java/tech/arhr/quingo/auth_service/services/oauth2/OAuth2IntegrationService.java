package tech.arhr.quingo.auth_service.services.oauth2;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.oauth2.OAuth2UserData;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import tech.arhr.quingo.auth_service.exceptions.persistence.EntityNotFoundException;
import tech.arhr.quingo.auth_service.services.SocialAccountService;
import tech.arhr.quingo.auth_service.services.UserService;

@Service
@RequiredArgsConstructor
public class OAuth2IntegrationService {
    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public UserDto processOAuth2User(OAuth2UserData userData) {
        try {
            SocialAccountDto account = socialAccountService.findByProviderAndProviderUserId(
                    userData.getProvider(),
                    userData.getProviderUserId());
            return userService.getUserById(account.getUserId());
        } catch (EntityNotFoundException e) {
            return handleNewSocialAccountLink(userData);
        }
    }

    @Transactional
    protected UserDto handleNewSocialAccountLink(OAuth2UserData userData) {
        try {
            UserDto user = userService.getUserByEmail(userData.getEmail());
            if (!userData.isEmailVerified()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("none"),
                        "We can't merge your account with unverified provider account email. Please use password authentication.");
            }
            if (!user.isEmailVerified()) {
                userService.clearUserPassword(user.getId());
                publisher.publishEvent(new AllUserSessionsInvalidatedEvent(user.getId()));
            }
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        } catch (EntityNotFoundException e1) {
            UserDto user = userService.createUserFromOAuth2(userData);
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        }
    }
}
