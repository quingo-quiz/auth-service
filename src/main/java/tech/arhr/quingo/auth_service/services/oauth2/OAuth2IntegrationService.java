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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OAuth2IntegrationService {
    private final UserService userService;
    private final SocialAccountService socialAccountService;
    private final ApplicationEventPublisher publisher;

    @Transactional
    public UserDto processOAuth2User(OAuth2UserData userData) {
        Optional<SocialAccountDto> opt = socialAccountService.findByProviderAndProviderUserIdOptional(
                userData.getProvider(),
                userData.getProviderUserId());

        if (opt.isPresent()) {
            SocialAccountDto account = opt.get();
            return userService.getUserById(account.getUserId());
        } else {
            return handleNewSocialAccountLink(userData);

        }
    }

    @Transactional
    protected UserDto handleNewSocialAccountLink(OAuth2UserData userData) {
        Optional<UserDto> opt = userService.getUserByEmailOptional(userData.getEmail());

        if (opt.isPresent()) {
            UserDto user = opt.get();
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
        } else {
            UserDto user = userService.createUserFromOAuth2(userData);
            socialAccountService.linkSocialAccount(userData, user.getId());
            return user;
        }
    }
}
