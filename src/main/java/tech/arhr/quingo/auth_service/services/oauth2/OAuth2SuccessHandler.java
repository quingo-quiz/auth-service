package tech.arhr.quingo.auth_service.services.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.TokenService;
import tech.arhr.quingo.auth_service.services.UserService;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    @Value("${spring.application.frontend-url}")
    private String frontendUrl;

    private final TokenService tokenService;
    private final UserService userService;
    private final CreateCookie createCookie;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        UserDto userDto = userService.getUserByEmail(oAuth2User.getAttributes().get("email").toString());
        TokenDto refresh = tokenService.createRefreshToken(userDto);
        TokenDto access = tokenService.createAccessToken(userDto);


        response.addHeader("Set-Cookie", createCookie.createAccessCookie(access).toString());
        response.addHeader("Set-Cookie", createCookie.createRefreshCookie(refresh).toString());

        getRedirectStrategy().sendRedirect(request, response, frontendUrl);
    }
}
