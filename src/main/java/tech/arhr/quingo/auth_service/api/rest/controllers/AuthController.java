package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.services.AuthService;

@RestController
@RequiredArgsConstructor
@Log
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @PostMapping("/auth")
    public ResponseEntity<?> login(@RequestBody @Valid AuthRequest authRequest) {
        AuthResponse authResponse = authService.authenticate(authRequest);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token") String refreshToken) {
        AuthResponse authResponse = authService.refresh(refreshToken);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@CookieValue(name = "refresh_token") String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout/all")
    public ResponseEntity<?> logoutAll(@CookieValue(name = "refresh_token") String refreshToken) {
        authService.logoutAll(refreshToken);
        return ResponseEntity.ok().build();
    }
}
