package tech.arhr.quingo.auth_service.data.redis.interfaces;

import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;

import java.util.Optional;

public interface RedisVerificationTokenRepository {
    void save(VerificationTokenRedisModel token);

    void delete(String token, VerificationTokenType type);

    Optional<VerificationTokenRedisModel> get(String token, VerificationTokenType type);
}
