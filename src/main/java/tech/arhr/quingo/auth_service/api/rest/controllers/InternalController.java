package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.AuthService;

@RestController
@RequestMapping("/internal")
@RequiredArgsConstructor
public class InternalController {
    private final AuthService authService;

    @PostMapping("/authorize")
    public ResponseEntity<UserDto> validate(@CookieValue(name = "access_token") String accessToken) {
        UserDto userDto = authService.authorize(accessToken);
        return ResponseEntity.ok().body(userDto);
    }
}
