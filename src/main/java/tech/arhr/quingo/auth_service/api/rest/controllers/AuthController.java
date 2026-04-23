package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.AuthResponseModel;
import tech.arhr.quingo.auth_service.api.rest.models.RefreshTokenRequest;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.api.rest.models.TokenModel;
import tech.arhr.quingo.auth_service.api.rest.utils.AuthStrategy;
import tech.arhr.quingo.auth_service.api.rest.utils.CreateCookie;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.dto.auth.AuthRequest;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.dto.auth.RegisterRequest;
import tech.arhr.quingo.auth_service.exceptions.auth.TokenNotFoundException;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthService authService;
    private final CreateCookie createCookie;
    private final TimeProvider timeProvider;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> register(
            @RequestBody @Valid RegisterRequest registerRequest,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        AuthResponse authResponse = authService.register(registerRequest);
        return createSuccessAuthResponse(authResponse, strategy);
    }

    @PostMapping("/auth")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> login(
            @RequestBody @Valid AuthRequest authRequest,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        AuthResponse authResponse = authService.authenticate(authRequest);
        return createSuccessAuthResponse(authResponse, strategy);
    }

    @PostMapping("/refresh")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        String refreshToken = resolveRefreshToken(refreshTokenFromCookie, refreshTokenRequest, strategy);
        AuthResponse authResponse = authService.refresh(refreshToken);

        return createSuccessAuthResponse(authResponse, strategy);
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @CookieValue(name = "access_token", required = false) String accessToken,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        String refreshToken = resolveRefreshToken(refreshTokenFromCookie, refreshTokenRequest, strategy);
        authService.logout(refreshToken, accessToken);
        return createLogoutResponse(strategy);
    }

    @PostMapping("/logout/all")
    public ResponseEntity<SuccessResponse<Void>> logoutAll(
            @CookieValue(name = "refresh_token", required = false) String refreshTokenFromCookie,
            @RequestBody(required = false) RefreshTokenRequest refreshTokenRequest,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        String refreshToken = resolveRefreshToken(refreshTokenFromCookie, refreshTokenRequest, strategy);
        authService.logoutAll(refreshToken);
        return createLogoutResponse(strategy);
    }

    @GetMapping("/sessions")
    public ResponseEntity<SuccessResponse<List<TokenModel>>> getSessions() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        List<TokenModel> tokens = authService.getActiveRefreshTokens(auth.getUser().getId());
        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        tokens,
                        timeProvider.now()
                )
        );
    }

    @PostMapping("/mfa/otp/verify")
    public ResponseEntity<SuccessResponse<AuthResponseModel>> otpVerify(
            @Valid @RequestBody OtpVerifyRequest request,
            @RequestHeader(value = "Auth-Strategy", defaultValue = "cookie") String strategyHeader) {

        AuthStrategy strategy = AuthStrategy.fromString(strategyHeader);
        AuthResponse response = authService.verifyOtpIssueTokens(request);

        return createSuccessAuthResponse(response, strategy);
    }


    private ResponseEntity<SuccessResponse<AuthResponseModel>> createSuccessAuthResponse(
            AuthResponse authResponse, AuthStrategy strategy) {

        HttpStatus status = authResponse.isMfaRequired()? HttpStatus.FORBIDDEN : HttpStatus.OK;

        if (authResponse.isMfaRequired()) {
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(SuccessResponse.of(
                            status,
                            AuthResponseModel.from(authResponse),
                            timeProvider.now()
                    ));
        }

        if (strategy.isJson()) {
            return ResponseEntity
                    .status(status)
                    .body(SuccessResponse.of(
                            status,
                            AuthResponseModel.from(authResponse),
                            timeProvider.now()));
        } else {
            ResponseCookie accessCookie = createCookie.createAccessCookie(authResponse.getAccessToken());
            ResponseCookie refreshCookie = createCookie.createRefreshCookie(authResponse.getRefreshToken());
            return ResponseEntity
                    .status(status)
                    .header("Set-Cookie", accessCookie.toString())
                    .header("Set-Cookie", refreshCookie.toString())
                    .body(SuccessResponse.of(
                            status,
                            null,
                            timeProvider.now()));
        }
    }

    private ResponseEntity<SuccessResponse<Void>> createLogoutResponse(AuthStrategy strategy) {
        ResponseCookie destroyRefresh = createCookie.createDestroyRefreshCookie();
        ResponseCookie destroyAccess = createCookie.createDestroyAccessCookie();

        return ResponseEntity.ok()
                .header("Set-Cookie", destroyAccess.toString())
                .header("Set-Cookie", destroyRefresh.toString())
                .body(SuccessResponse.of(HttpStatus.OK, null, timeProvider.now()));
    }

    private String resolveRefreshToken(String fromCookie, RefreshTokenRequest fromBody, AuthStrategy strategy) {
        if (strategy.isJson()) {
            if (fromBody == null || fromBody.getRefreshToken() == null) {
                throw new TokenNotFoundException("Refresh token required in body for json strategy");
            }
            return fromBody.getRefreshToken();
        } else {
            if (fromCookie == null) {
                throw new TokenNotFoundException("Refresh token cookie required");
            }
            return fromCookie;
        }
    }
}