package tech.arhr.quingo.auth_service.data.redis.models;

import lombok.Data;

import java.util.UUID;

@Data
public class VerificationTokenRedisModel {
    public static String prefix = "verificationToken";

    private String token;
    private UUID userId;
}
