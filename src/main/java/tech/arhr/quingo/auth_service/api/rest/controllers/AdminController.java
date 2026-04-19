package tech.arhr.quingo.auth_service.api.rest.controllers;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.ChangeUserRolesRequest;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserService userService;
    private final TimeProvider timeProvider;

    @PostMapping("/users/{userId}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> blockUser(
            @PathVariable UUID userId
    ) {
        userService.blockUser(userId);
        return ResponseEntity.ok(SuccessResponse.of(
                HttpStatus.OK,
                null,
                timeProvider.now()
        ));
    }

    @PostMapping("/users/{userId}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> unblockUser(
            @PathVariable UUID userId
    ) {
        userService.unblockUser(userId);
        return ResponseEntity.ok(SuccessResponse.of(
                HttpStatus.OK,
                null,
                timeProvider.now()
        ));
    }

    @PatchMapping("/users/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Void>> changeUserRoles(
            @PathVariable UUID userId,
            @RequestBody @Valid ChangeUserRolesRequest request
    ) {
        userService.changeUserRoles(userId, request.getUserRoles());
        return ResponseEntity.ok(SuccessResponse.of(
                HttpStatus.OK,
                null,
                timeProvider.now()
        ));
    }
}
