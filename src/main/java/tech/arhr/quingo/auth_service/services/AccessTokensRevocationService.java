package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AccessTokensRevocationService {
    private final RedisTemplate<String, Instant> revocationTimeRedisTemplate;
    private final TimeProvider timeProvider;
    private static final String SESSION_PREFIX = "valid_after:session:";
    private static final String USER_PREFIX = "valid_after:user:";
    @Value("${spring.jwt.expiration.access-minutes}")
    private int accessTtlMinutes;


    public boolean isAccessTokenBlocked(UUID userId, UUID sessionId, Instant issuedAt) {
        String sessionKey = SESSION_PREFIX + sessionId;
        String userKey = USER_PREFIX + userId;

        Instant sessionRevocationTime = revocationTimeRedisTemplate.opsForValue().get(sessionKey);
        Instant userRevocationTime = revocationTimeRedisTemplate.opsForValue().get(userKey);

        if (sessionRevocationTime != null && !issuedAt.isAfter(sessionRevocationTime)) {
            return true;
        }

        if (userRevocationTime != null && !issuedAt.isAfter(userRevocationTime)) {
            return true;
        }

        return false;
    }

    public void invalidateSessionTokens(UUID sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Instant value = timeProvider.now();

        revocationTimeRedisTemplate.opsForValue().set(key, value, accessTtlMinutes, TimeUnit.MINUTES);
    }

    public void invalidateAllUserTokens(UUID userId) {
        String key = USER_PREFIX + userId;
        Instant value = timeProvider.now();

        revocationTimeRedisTemplate.opsForValue().set(key, value, accessTtlMinutes, TimeUnit.MINUTES);
    }
}
