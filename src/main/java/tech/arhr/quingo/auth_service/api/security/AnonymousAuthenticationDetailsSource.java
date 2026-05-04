package tech.arhr.quingo.auth_service.api.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationDetailsSource;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnonymousAuthenticationDetailsSource
        implements AuthenticationDetailsSource<Object, WebAuthenticationDetails> {

    @Override
    public WebAuthenticationDetails buildDetails(Object context) {
        if (context instanceof jakarta.servlet.http.HttpServletRequest request) {
            return new CustomWebAuthenticationDetails(request);
        }
        return new WebAuthenticationDetails(null, null);
    }
}