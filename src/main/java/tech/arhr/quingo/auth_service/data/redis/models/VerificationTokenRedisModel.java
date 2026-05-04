package tech.arhr.quingo.auth_service.data.redis.models;

import lombok.Data;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;

import java.util.UUID;

@Data
public class VerificationTokenRedisModel {
    private String token;
    private UUID userId;
    private VerificationTokenType type;
}
