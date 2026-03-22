package tech.arhr.quingo.auth_service.data.redis.models;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class TokenRedisModel {
    public static String prefix = "token";

    private UUID tokenId;
    private UUID userId;
    private Instant expireTime;
}
