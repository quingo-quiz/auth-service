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

@Repository
public class RedisTokenRepositoryImpl implements RedisTokenRepository {
    private final RedisTemplate<String, TokenRedisModel> tokenRedisTemplate;
    private final String TOKEN_PREFIX = "token";
    private final String USER_PREFIX = "user";

    @Value("${spring.jwt.expiration.access-minutes}")
    private int accessTtlMinutes;


    @Autowired
    public RedisTokenRepositoryImpl(
            RedisTemplate<String, TokenRedisModel> tokenRedisTemplate) {
        this.tokenRedisTemplate = tokenRedisTemplate;
    }

    @Override
    public void save(TokenRedisModel token) {
        String key = createTokenKey(token.getTokenId());

        tokenRedisTemplate.opsForValue().set(key,
                token,
                accessTtlMinutes,
                TimeUnit.MINUTES);
    }

    @Override
    public Optional<TokenRedisModel> read(UUID tokenId) {
        String key = createTokenKey(tokenId);
        TokenRedisModel model = tokenRedisTemplate.opsForValue().get(key);

        return Optional.ofNullable(model);
    }

    @Override
    public void delete(UUID tokenId) {
        String key = createTokenKey(tokenId);
        tokenRedisTemplate.delete(key);
    }

    @Override
    public boolean exists(UUID tokenId) {
        String key = createTokenKey(tokenId);
        return tokenRedisTemplate.hasKey(key);
    }

    @Override
    public void addToUserTokensSet(UUID userId, TokenRedisModel token) {
        String key = createUserTokensKey(userId);
        tokenRedisTemplate.opsForSet().add(key, token);
        tokenRedisTemplate.expire(key, accessTtlMinutes, TimeUnit.MINUTES);
    }

    @Override
    public void removeFromUserTokensSet(UUID userId, TokenRedisModel token) {
        String key = createUserTokensKey(userId);
        tokenRedisTemplate.opsForSet().remove(key, token);
    }

    @Override
    public void revokeAllUserTokensFromSet(UUID userId) {
        String key = createUserTokensKey(userId);

        Set<TokenRedisModel> tokens = tokenRedisTemplate.opsForSet().members(key);

        if (tokens != null) {
            tokens.stream()
                    .forEach((token) -> {
                        delete(token.getTokenId());
                    });
        }

        tokenRedisTemplate.delete(key);
    }

    @Override
    public Set<TokenRedisModel> getAllUserTokensSet(UUID userId) {
        String key = createUserTokensKey(userId);
        Set<TokenRedisModel> tokens = tokenRedisTemplate.opsForSet().members(key);
        tokens = tokens == null ? Collections.emptySet() : tokens;

        return tokens;
    }

    @Override
    public void setAllUserTokensSet(UUID userId, Set<TokenRedisModel> tokens) {
        String key = createUserTokensKey(userId);

        tokenRedisTemplate.delete(key);

        if (tokens != null && !tokens.isEmpty()) {
            TokenRedisModel[] tokenArray = tokens.toArray(new TokenRedisModel[0]);
            tokenRedisTemplate.opsForSet().add(key, tokenArray);
            tokenRedisTemplate.expire(key, accessTtlMinutes, TimeUnit.MINUTES);
        }
    }

    private String createTokenKey(UUID tokenId) {
        return keyBuilder(tokenId, TOKEN_PREFIX);
    }

    private String createUserTokensKey(UUID userId) {
        return keyBuilder(userId, USER_PREFIX, TOKEN_PREFIX);
    }

    private String keyBuilder(UUID id, String... prefixes){
        StringBuilder sb = new StringBuilder();
        if (prefixes != null){
            for(String prefix : prefixes){
                sb.append(prefix);
                sb.append(":");
            }
        }
        sb.append(id.toString());
        return sb.toString();
    }

}
