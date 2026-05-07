package tech.arhr.quingo.auth_service.services.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackCode;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackStatus;
import tech.arhr.quingo.auth_service.utils.callbacks.CallbackUrlBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final CallbackUrlBuilder  callbackUrlBuilder;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {

        String errorMessage = exception.getMessage();
        String targetUrl = callbackUrlBuilder.buildCallbackUrl(
                CallbackStatus.ERROR,
                CallbackCode.OAUTH2_FAILED,
                errorMessage
        );

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
