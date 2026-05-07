package tech.arhr.quingo.auth_service.services.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.api.security.CustomWebAuthenticationDetails;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.SessionTokens;
import tech.arhr.quingo.auth_service.services.SessionService;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackCode;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackStatus;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackUrlBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final CallbackUrlBuilder callbackUrlBuilder;

    private final SessionService sessionService;
    private final UserService userService;
    private final CreateCookie createCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        UserDto userDto = userService.getUserByEmail(oAuth2User.getAttributes().get("email").toString());
        SessionTokens sessionTokens = sessionService.createSession(userDto, getClientInfoFromContext());
        TokenDto refresh = sessionTokens.getRefreshToken();
        TokenDto access = sessionTokens.getAccessToken();

        response.addHeader("Set-Cookie", createCookie.createAccessCookie(access).toString());
        response.addHeader("Set-Cookie", createCookie.createRefreshCookie(refresh).toString());

        String targetUri = callbackUrlBuilder.buildCallbackUrl(
                CallbackStatus.SUCCESS,
                CallbackCode.OAUTH2_SUCCESS,
                ""
        );

        getRedirectStrategy().sendRedirect(request, response, targetUri);
    }

    private UserAgentInfoDto getClientInfoFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getDetails() instanceof CustomWebAuthenticationDetails details) {
            return details.getUserAgentInfo();
        }
        return new UserAgentInfoDto();
    }
}
