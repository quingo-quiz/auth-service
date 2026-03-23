package tech.arhr.quingo.auth_service.data.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;

import java.util.concurrent.TimeUnit;

@Repository
public class RedisVerificationTokenRepositoryImpl implements RedisVerificationTokenRepository {
    private final RedisTemplate<String, VerificationTokenRedisModel> redisTemplate;
    private final String TOKEN_PREFIX = VerificationTokenRedisModel.prefix;

    // TODO add config value
    private int tokenTtlMinutes = 60;

    public RedisVerificationTokenRepositoryImpl(
            RedisTemplate<String, VerificationTokenRedisModel> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(VerificationTokenRedisModel model) {
        String key = createKey(model.getToken());
        redisTemplate.opsForValue().set(
                key,
                model,
                tokenTtlMinutes,
                TimeUnit.MINUTES);
    }

    @Override
    public boolean exists(String token) {
        String key = createKey(token);
        return redisTemplate.hasKey(key);
    }

    private String createKey(String token) {
        return TOKEN_PREFIX + ":" + token;
    }
}
