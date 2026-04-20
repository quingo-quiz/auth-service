package tech.arhr.quingo.auth_service.dto;

import jakarta.persistence.Column;
import lombok.Data;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

import java.util.UUID;

@Data
public class SocialAccountDto {
    private UUID id;
    private UUID userId;
    private OAuth2Provider provider;
    private String providerUserId;
}
