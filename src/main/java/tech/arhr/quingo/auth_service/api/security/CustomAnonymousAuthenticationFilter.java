package tech.arhr.quingo.auth_service.api.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.stereotype.Component;

@Component
public class CustomAnonymousAuthenticationFilter extends AnonymousAuthenticationFilter {

    @Autowired
    private AnonymousAuthenticationDetailsSource detailsSource;

    public CustomAnonymousAuthenticationFilter() {
        super("anonymousKey");
    }

    public CustomAnonymousAuthenticationFilter(String key, AnonymousAuthenticationDetailsSource detailsSource) {
        super(key);
        this.detailsSource = detailsSource;
    }

    @Override
    protected Authentication createAuthentication(HttpServletRequest request) {
        AnonymousAuthenticationToken auth = (AnonymousAuthenticationToken) super.createAuthentication(request);
        auth.setDetails(detailsSource.buildDetails(request));
        return auth;
    }
}