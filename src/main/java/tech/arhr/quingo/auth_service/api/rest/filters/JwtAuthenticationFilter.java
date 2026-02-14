package tech.arhr.quingo.auth_service.api.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;
import tech.arhr.quingo.auth_service.exceptions.auth.TokenNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
@Lazy
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private AuthenticationManager authenticationManager;

    @Autowired
    public void setAuthenticationManager(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (request.getCookies() == null || request.getCookies().length == 0) {
            filterChain.doFilter(request, response);
        } else {

            List<Cookie> cookies = List.of(request.getCookies());

            cookies.stream()
                    .filter(cookie -> cookie.getName().equals("access_token"))
                    .findFirst()
                    .ifPresent((cookie) -> {
                        authInManager(cookie.getValue());
                    });

            filterChain.doFilter(request, response);
        }
    }

    private void authInManager(String token) {
        if (token == null || token.isEmpty()) return;
        try {
            Authentication authentication = new JwtAuthenticationToken(token);
            Authentication result = authenticationManager.authenticate(authentication);
            SecurityContextHolder.getContext().setAuthentication(result);
        } catch (AuthException e) {}
    }

}
