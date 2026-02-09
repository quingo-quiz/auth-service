package tech.arhr.quingo.auth_service.api.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import tech.arhr.quingo.auth_service.dto.TokenDto;

public class CreateCookie {

    @Value(value = "${spring.application.domain:localhost}")
    private static String domain;

    private static final Long accessExpiration = 60*15L;

    private static final Long refreshExpiration = 60 * 60 * 24 * 30L;

    public static ResponseCookie createCookie(String name, String value, String path, Long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .maxAge(maxAgeSeconds)
                .path(path)
                .httpOnly(true)
                .secure(true)
                .domain(domain)
                .build();
    }

    public static ResponseCookie createAccessCookie(TokenDto accessToken) {
        return CreateCookie.createCookie(
                "access_token",
                accessToken.getToken(),
                "/",
                accessExpiration);
    }

    public static ResponseCookie createRefreshCookie(TokenDto refreshToken) {
        return CreateCookie.createCookie(
                "refresh_token",
                refreshToken.getToken(),
                "/auth",
                refreshExpiration);
    }
}
