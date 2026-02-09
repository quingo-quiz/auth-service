package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.arhr.quingo.auth_service.api.rest.dto.RegisterRequest;
import tech.arhr.quingo.auth_service.dto.TokenPairDto;
import tech.arhr.quingo.auth_service.services.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> auth(RegisterRequest registerRequest) {
        TokenPairDto tokenPairDto = authService.register(registerRequest);

        return ResponseEntity.ok().build();
    }




}
