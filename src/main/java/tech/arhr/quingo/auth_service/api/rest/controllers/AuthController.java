package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.models.AuthResponseModel;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.util.logging.Level;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final CreateCookie createCookie;
    private final TimeProvider  timeProvider;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> register(@RequestBody @Valid RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);

        ResponseCookie accessCookie = createCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = createCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(SuccessResponse.of(HttpStatus.OK,
                        AuthResponseModel.from(authResponse),
                        timeProvider.now()));
    }

    @PostMapping("/auth")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> login(@RequestBody @Valid AuthRequest authRequest) {
        AuthResponse authResponse = authService.authenticate(authRequest);

        ResponseCookie accessCookie = createCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = createCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(SuccessResponse.of(HttpStatus.OK,
                        AuthResponseModel.from(authResponse),
                        timeProvider.now()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> refresh(@CookieValue(name = "refresh_token") String refreshToken) {
        AuthResponse authResponse = authService.refresh(refreshToken);

        ResponseCookie accessCookie = createCookie.createAccessCookie(authResponse.getAccessToken());
        ResponseCookie refreshCookie = createCookie.createRefreshCookie(authResponse.getRefreshToken());

        return ResponseEntity
                .ok()
                .header("Set-Cookie", accessCookie.toString())
                .header("Set-Cookie", refreshCookie.toString())
                .body(SuccessResponse.of(HttpStatus.OK,
                        AuthResponseModel.from(authResponse),
                        timeProvider.now()));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(@CookieValue(name = "refresh_token") String refreshToken,
                                                        @CookieValue(name = "access_token", required = false) String accessToken) {
        authService.logout(refreshToken, accessToken);
        ResponseCookie destroyRefresh = createCookie.createDestroyRefreshCookie();
        ResponseCookie destroyAccess = createCookie.createDestroyAccessCookie();
        return ResponseEntity
                .ok()
                .header("Set-Cookie", destroyAccess.toString())
                .header("Set-Cookie", destroyRefresh.toString())
                .body(SuccessResponse.of(HttpStatus.OK, null, timeProvider.now()));
    }

    @PostMapping("/logout/all")
    public ResponseEntity<SuccessResponse<Void>> logoutAll(@CookieValue(name = "refresh_token") String refreshToken) {
        authService.logoutAll(refreshToken);
        ResponseCookie destroyRefresh = createCookie.createDestroyRefreshCookie();
        ResponseCookie destroyAccess = createCookie.createDestroyAccessCookie();
        return ResponseEntity
                .ok()
                .header("Set-Cookie", destroyAccess.toString())
                .header("Set-Cookie", destroyRefresh.toString())
                .body(SuccessResponse.of(HttpStatus.OK, null, timeProvider.now()));
    }
}
