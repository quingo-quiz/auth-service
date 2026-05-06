package tech.arhr.quingo.auth_service.data.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisVerificationTokenRepositoryImpl implements RedisVerificationTokenRepository {
    private final RedisTemplate<String, VerificationTokenRedisModel> redisTemplate;

    // TODO add config value
    private final int tokenTtlMinutes = 60;

    public RedisVerificationTokenRepositoryImpl(
            RedisTemplate<String, VerificationTokenRedisModel> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(VerificationTokenRedisModel model) {
        String key = createKey(model);
        redisTemplate.opsForValue().set(
                key,
                model,
                tokenTtlMinutes,
                TimeUnit.MINUTES);
    }

    @Override
    public void delete(String token, VerificationTokenType type) {
        String key = createKey(token, type);
        redisTemplate.delete(key);
    }

    @Override
    public Optional<VerificationTokenRedisModel> get(String token, VerificationTokenType type) {
        String key = createKey(token, type);
        return Optional.ofNullable(redisTemplate.opsForValue().get(key));
    }

    private String createKey(VerificationTokenRedisModel model) {
        return model.getType().getPrefix() + ":" + model.getToken();
    }

    private String createKey(String token, VerificationTokenType type) {
        return type.getPrefix() + ":" + token;
    }
}
