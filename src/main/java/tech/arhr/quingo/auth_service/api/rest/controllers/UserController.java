package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.services.UserService;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/info")
    private ResponseEntity<UserDto> validate(@CookieValue(name = "access_token") String accessToken) {
        // only for testing
        // remove
        UserDto user = authService.authorize(accessToken);
        return ResponseEntity.ok(userService.getUserById(user.getId()));
    }
}
