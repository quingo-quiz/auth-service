package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.security.JwtAuthenticationToken;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;
import tech.arhr.quingo.auth_service.services.UserService;

@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    @GetMapping("/info")
    public ResponseEntity<UserDto> info() {
        JwtAuthenticationToken auth = (JwtAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();

       return ResponseEntity.ok(auth.getUser());
    }

    // for testing permissions
    @GetMapping("/info/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserDto> admin(@CookieValue(name = "access_token") String accessToken) {
        UserDto user = authService.authorize(accessToken);
        return ResponseEntity.ok(userService.getUserById(user.getId()));
    }
}
