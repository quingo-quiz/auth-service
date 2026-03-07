package tech.arhr.quingo.auth_service.data.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedisTokenRepositoryImpl implements RedisTokenRepository {
    private final RedisTemplate<String, Object> template;
    private final String TOKEN_PREFIX = "token";

    @Value("${spring.jwt.expiration.access-minutes}")
    private int configAccessMinutes;

    private final long accessTtlSeconds;

    @Autowired
    public RedisTokenRepositoryImpl(
            RedisTemplate<String, Object> template) {
        this.template = template;

        accessTtlSeconds = configAccessMinutes * 60L * 2;
    }

    @Override
    public void save(String jti) {
        template.opsForValue().set(jti, jti, accessTtlSeconds);
    }
}
