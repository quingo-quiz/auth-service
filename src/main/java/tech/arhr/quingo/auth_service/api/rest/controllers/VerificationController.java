package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.services.VerificationService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

@RestController
@RequiredArgsConstructor
@RequestMapping("/verification")
public class VerificationController {
    private final UserService userService;
    private final VerificationService verificationService;
    private final TimeProvider timeProvider;

    @PatchMapping("/email/{token}")
    public ResponseEntity<SuccessResponse<Void>> verifyToken(
            @PathVariable String token
    ) {
        userService.verifyEmail(token);

        return ResponseEntity
                .ok()
                .body(SuccessResponse.of(
                        HttpStatus.OK,
                        null,
                        timeProvider.now()));
    }

    @PostMapping("/email/resend")
    public ResponseEntity<SuccessResponse<Void>> resendEmailVerification(){
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
