package tech.arhr.quingo.auth_service.data.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Repository
public class RedisTokenRepositoryImpl implements RedisTokenRepository {
    private final RedisTemplate<String, Object> template;
    private final String TOKEN_PREFIX = "token";
    private final String USER_PREFIX = "user";

    @Value("${spring.jwt.expiration.access-minutes}")
    private int accessTtlMinutes;


    @Autowired
    public RedisTokenRepositoryImpl(
            RedisTemplate<String, Object> template) {
        this.template = template;
    }

    @Override
    public void save(TokenRedisModel token) {
        String key = createTokenKey(token.getTokenId());

        template.opsForValue().set(key,
                token,
                accessTtlMinutes,
                TimeUnit.MINUTES);
    }

    @Override
    public Optional<TokenRedisModel> read(UUID tokenId) {
        String key = createTokenKey(tokenId);
        TokenRedisModel model = (TokenRedisModel) template.opsForValue().get(key);

        return Optional.ofNullable(model);
    }

    @Override
    public void delete(UUID tokenId) {
        String key = createTokenKey(tokenId);
        template.delete(key);
    }

    @Override
    public boolean exists(UUID tokenId) {
        String key = createTokenKey(tokenId);
        return template.hasKey(key);
    }

    @Override
    public void addToUserTokensSet(UUID userId, TokenRedisModel token) {
        String key = createUserTokensKey(userId);
        template.opsForSet().add(key, token);
        template.expire(key, accessTtlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void removeFromUserTokensSet(UUID userId, TokenRedisModel token) {
        String key = createUserTokensKey(userId);
        template.opsForSet().remove(key, token);
    }

    @Override
    public void revokeAllUserTokensFromSet(UUID userId) {
        String key = createUserTokensKey(userId);

        Set<Object> tokens = template.opsForSet().members(key);

        if (tokens != null) {
            tokens.stream()
                    .map((token) -> (TokenRedisModel) token)
                    .forEach((token) -> {
                        delete(token.getTokenId());
                    });
        }

        template.delete(key);
    }

    @Override
    public Set<TokenRedisModel> getAllUserTokensSet(UUID userId) {
        String key = createUserTokensKey(userId);
        Set<Object> tokens = template.opsForSet().members(key);
        tokens = tokens == null ? Collections.emptySet() : tokens;

        return tokens.stream()
                .map((token) -> (TokenRedisModel) token)
                .collect(Collectors.toSet());
    }

    @Override
    public void setAllUserTokensSet(UUID userId, Set<TokenRedisModel> tokens) {
        String key = createUserTokensKey(userId);

        template.delete(key);
        template.opsForSet().add(key, tokens);
        template.expire(key, accessTtlMinutes, TimeUnit.MINUTES);
    }

    // todo переделать
    private String createTokenKey(UUID tokenId) {
        if (tokenId == null)
            throw new RuntimeException("tokenId is null");

        return TOKEN_PREFIX +
                ":" +
                tokenId.toString();
    }

    // todo переделать
    private String createUserTokensKey(UUID userId) {
        if (userId == null)
            throw new RuntimeException("userId is null");

        return TOKEN_PREFIX +
                ":" +
                USER_PREFIX +
                ":" +
                userId.toString();
    }

}
