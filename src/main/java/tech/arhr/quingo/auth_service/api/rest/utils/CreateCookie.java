package tech.arhr.quingo.auth_service.api.rest.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;

public class CreateCookie {

    @Value("${domain:localhost}")
    private static String domain;

    public static ResponseCookie create(String name, String value, String path, Long maxAge) {
        return ResponseCookie.from(name, value)
                .maxAge(maxAge)
                .path(path)
                .httpOnly(true)
                .secure(true)
                .domain(domain)
                .build();
    }
}
