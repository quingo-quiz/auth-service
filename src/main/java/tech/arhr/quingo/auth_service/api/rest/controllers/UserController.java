package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.*;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.dto.SecurityStatusDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.services.SecurityService;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.services.VerificationService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;
    private final VerificationService verificationService;
    private final TimeProvider timeProvider;
    private final SecurityService securityService;

    @GetMapping("/info")
    public ResponseEntity<SuccessResponse<UserDto>> info() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        UserDto user = userService.getUserById(auth.getUser().getId());
        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        user,
                        timeProvider.now()
                ));
    }

    @PostMapping("/password/change")
    public ResponseEntity<SuccessResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        authService.changePassword(
                auth.getUser().getId(),
                request.getOldPassword(),
                request.getNewPassword());

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()
                ));
    }

    @PostMapping("/password/set")
    public ResponseEntity<SuccessResponse<Void>> setPassword(
            @RequestBody @Valid SetPasswordRequest request
    ) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        authService.setPassword(auth.getUser().getId(), request.getPassword());

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()
                ));
    }

    @GetMapping("/security-status")
    public ResponseEntity<SuccessResponse<SecurityStatusDto>> getSecurityStatus() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        SecurityStatusDto status = securityService.getUserSecurityStatus(auth.getUser().getId());

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        status,
                        timeProvider.now()
                ));
    }

    @PostMapping("/password/send-reset")
    public ResponseEntity<SuccessResponse<Void>> resetPassword(
            @Valid @RequestBody SendResetPasswordRequest request
    ) {
        String email = request.getEmail();
        userService.sendResetPassword(email);

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()
                ));
    }

    @PatchMapping("/password/reset")
    public ResponseEntity<SuccessResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(request.getResetToken(), request.getNewPassword());

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()
                ));
    }

    @PatchMapping("/email/verify")
    public ResponseEntity<SuccessResponse<Void>> verifyToken(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        userService.verifyEmail(request.getVerificationToken());

        return ResponseEntity
                .ok()
                .body(SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()));
    }

    @PostMapping("/email/verify/resend")
    public ResponseEntity<SuccessResponse<Void>> resendEmailVerification() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        verificationService.sendVerificationEmail(auth.getUser());

        return ResponseEntity
                .ok()
                .body(SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()));
    }
}
