package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.ChangePasswordRequest;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.dto.SecurityStatusDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.services.SecurityService;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;
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

    @PostMapping("/change-password")
    public ResponseEntity<SuccessResponse<Void>> changePassword(
            @RequestBody @Valid ChangePasswordRequest request
    ) {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        authService.changePassword(auth.getUser().getId(), request);

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
}
