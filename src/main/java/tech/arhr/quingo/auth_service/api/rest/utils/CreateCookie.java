package tech.arhr.quingo.auth_service.api.rest.utils;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.dto.TokenDto;

@Component
public class CreateCookie {

    @Value(value = "${spring.application.domain}")
    private String domain;

    @Value("${spring.jwt.expiration.access-minutes}")
    private int ACCESS_EXPIRATION_MINUTES;

    @Value("${spring.jwt.expiration.refresh-days}")
    private int REFRESH_EXPIRATION_DAYS;

    public ResponseCookie createCookie(String name, String value, String path, Long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .maxAge(maxAgeSeconds)
                .path(path)
                .httpOnly(true)
                .secure(true)
                .domain(domain)
                .build();
    }

    public ResponseCookie createAccessCookie(TokenDto accessToken) {
        return createCookie(
                "access_token",
                accessToken.getToken(),
                "/",
                ACCESS_EXPIRATION_MINUTES * 60L);
    }

    public ResponseCookie createDestroyAccessCookie() {
        return createCookie(
                "access_token",
                "null",
                "/",
                0L);
    }

    public ResponseCookie createRefreshCookie(TokenDto refreshToken) {
        return createCookie(
                "refresh_token",
                refreshToken.getToken(),
                "/auth",
                REFRESH_EXPIRATION_DAYS * 24 * 60 * 60L);
    }

    public ResponseCookie createDestroyRefreshCookie() {
        return createCookie(
                "refresh_token",
                "null",
                "/auth",
                0L);
    }
}
