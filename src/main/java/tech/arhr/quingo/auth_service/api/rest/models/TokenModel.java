package tech.arhr.quingo.auth_service.api.rest.models;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TokenModel {
    private UUID tokenId;
    private Instant issuedAt;
    private Instant expiresAt;

    private String browser;
    private String os;
    private String device;
    private String ipAddress;
}
