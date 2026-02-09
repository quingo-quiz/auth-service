package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.dto.AuthRequest;
import tech.arhr.quingo.auth_service.api.rest.dto.RegisterRequest;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.dto.TokenPairDto;
import tech.arhr.quingo.auth_service.services.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid RegisterRequest registerRequest) {
        TokenPairDto tokenPairDto = authService.register(registerRequest);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(tokenPairDto.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(tokenPairDto.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @PostMapping("/auth")
    public ResponseEntity<?> login(@Valid AuthRequest authRequest) {
        TokenPairDto tokenPairDto = authService.authenticate(authRequest);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(tokenPairDto.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(tokenPairDto.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .build();
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@CookieValue(name = "refresh_token") String refreshToken) {
        TokenPairDto tokenPairDto = authService.refresh(refreshToken);

        ResponseCookie accessCookie = CreateCookie.createAccessCookie(tokenPairDto.getAccessToken());
        ResponseCookie refreshCookie = CreateCookie.createRefreshCookie(tokenPairDto.getRefreshToken());

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
