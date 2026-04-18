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
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;
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

    @GetMapping("/info")
    public ResponseEntity<SuccessResponse<UserDto>> info() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        auth.getUser(),
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

    // for testing permissions
    @GetMapping("/info/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> admin(@CookieValue(name = "access_token") String accessToken) {
        UserDto user = authService.authorize(accessToken);
        return ResponseEntity.ok(userService.getUserById(user.getId()));
    }
}
