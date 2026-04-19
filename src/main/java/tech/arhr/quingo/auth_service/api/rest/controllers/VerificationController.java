package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.websocket.server.PathParam;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.services.VerificationService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

@RestController
@RequiredArgsConstructor
@RequestMapping("/verify")
public class VerificationController {
    private final VerificationService verificationService;
    private final TimeProvider timeProvider;

    @GetMapping("/{token}")
    public ResponseEntity<SuccessResponse<Boolean>> verifyToken(
            @PathVariable String token
    ) {
        boolean verified = verificationService.validateToken(token);
        HttpStatus status = verified ? HttpStatus.OK : HttpStatus.BAD_REQUEST;


        return ResponseEntity
                .status(status.value())
                .body(SuccessResponse.of(
                        status,
                        verified,
                        timeProvider.now()));
    }
}
