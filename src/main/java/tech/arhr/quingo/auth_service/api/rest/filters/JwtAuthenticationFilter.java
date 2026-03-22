package tech.arhr.quingo.auth_service.api.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationConfiguration authenticationConfiguration;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        log.info(request.getRequestURI());
        log.info(request.getMethod());


        Optional<Cookie> accessCookie = extractTokenCookie(request.getCookies());
        Optional<String> bearerToken = extractBearerToken(request.getHeader("Authorization"));
        String token = null;

        if (accessCookie.isPresent()) {
            token = accessCookie.get().getValue();
        } else if (bearerToken.isPresent()) {
            token = bearerToken.get();
        }

        log.info("token: " + token);

        if (token != null) {
            try {
                authInManager(token);
            } catch (AuthException e) {
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

    private Optional<Cookie> extractTokenCookie(Cookie[] cookies) {
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        Arrays.stream(cookies)
                .forEach(cookie -> log.info(cookie.getName() + " : " + cookie.getValue()));

        Optional<Cookie> accessCookie = Arrays.stream(cookies)
                .filter(cookie -> cookie.getName().equals("access_token"))
                .findFirst();

        return accessCookie;
    }

    private Optional<String> extractBearerToken(String authHeader) {
        if (authHeader == null || authHeader.isEmpty()) {
            return Optional.empty();
        }
        try {
            String[] parsed = authHeader.split("\\s");
            String bearerToken = parsed[1];
            return Optional.of(bearerToken);
        } catch (ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
    }
}