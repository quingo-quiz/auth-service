package tech.arhr.quingo.auth_service.api.rest.models;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class RefreshTokenApiModel {
    private UUID tokenId;
    private UUID sessionId;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant loggedInAt;
    private boolean isCurrent;

    private String browser;
    private String os;
    private String device;
    private String ipAddress;
}
