package tech.arhr.quingo.auth_service.api.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getCookies() == null || request.getCookies().length == 0) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<Cookie> accessCookie = Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals("access_token"))
                .findFirst();

        if (accessCookie.isPresent()) {
            try {
                authInManager(accessCookie.get().getValue());
            }catch (AuthException e){
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, new BadCredentialsException(e.getMessage(), e));
                return;
            } catch (AuthenticationException e) {
                SecurityContextHolder.clearContext();
                authenticationEntryPoint.commence(request, response, e);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    @SneakyThrows
    private void authInManager(String token) {
        AuthenticationManager manager = authenticationConfiguration.getAuthenticationManager();
        Authentication result = manager.authenticate(new JwtAuthenticationToken(token));
        SecurityContextHolder.getContext().setAuthentication(result);
    }
}