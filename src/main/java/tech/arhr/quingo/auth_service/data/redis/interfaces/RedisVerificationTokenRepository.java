package tech.arhr.quingo.auth_service.data.redis.interfaces;

import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;

public interface RedisVerificationTokenRepository {
    void save(VerificationTokenRedisModel token);

    boolean exists(String token);
}
