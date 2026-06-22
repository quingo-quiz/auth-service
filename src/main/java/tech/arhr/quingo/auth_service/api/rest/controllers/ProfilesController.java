package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.api.rest.models.SuccessResponse;
import tech.arhr.quingo.auth_service.dto.UserProfileDto;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfilesController {
    private final UserService userService;
    private final TimeProvider timeProvider;

    @GetMapping("/{id}")
    public ResponseEntity<SuccessResponse<UserProfileDto>> getProfile(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        userService.getUserProfile(id),
                        timeProvider.now()
                ));
    }

    @PostMapping("/batch")
    public ResponseEntity<SuccessResponse<List<UserProfileDto>>> getProfiles(
            @RequestBody List<UUID> ids
    ) {
        return ResponseEntity.ok(
                SuccessResponse.of(
                        HttpStatus.OK,
                        userService.getUserProfiles(ids),
                        timeProvider.now()
                ));
    }
}
