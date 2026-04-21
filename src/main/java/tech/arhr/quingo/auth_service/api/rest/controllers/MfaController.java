package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.dto.auth.OtpConnectDto;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

@RestController
@RequestMapping("/mfa")
@RequiredArgsConstructor
public class MfaController {
    private final MfaService mfaService;
    private final TimeProvider timeProvider;

    @PostMapping("/otp/connect")
    public ResponseEntity<SuccessResponse<OtpConnectDto>> otpInit() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        OtpConnectDto dto = mfaService.connectOtp(auth.getUser());
        return ResponseEntity.ok(SuccessResponse.of(
                HttpStatus.OK,
                dto,
                timeProvider.now()
        ));
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<SuccessResponse<Void>> otpVerify(@Valid @RequestBody OtpVerifyRequest request) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        mfaService.verifyOtpCode(auth.getUser(), request);

        return ResponseEntity.ok(SuccessResponse.of(
                HttpStatus.OK,
                null,
                timeProvider.now()
        ));
    }
}
