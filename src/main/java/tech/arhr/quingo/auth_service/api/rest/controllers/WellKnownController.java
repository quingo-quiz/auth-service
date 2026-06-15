package tech.arhr.quingo.auth_service.api.rest.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class WellKnownController {

    @Value("${spring.jwt.public-key}")
    private String publicKeyPem;

    @GetMapping(value = "/jwt-public-key", produces = "text/plain")
    public String getPublicKey() {
        return publicKeyPem;
    }
}
