package tech.arhr.quingo.auth_service.data.redis;

import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface RedisTokenRepository {
    void save(TokenRedisModel token);

    Optional<TokenRedisModel> read(UUID tokenId);

    void delete(UUID tokenId);

    boolean exists(UUID tokenId);

    void addToUserTokensSet(UUID userId, TokenRedisModel token);

    void removeFromUserTokensSet(UUID userId, TokenRedisModel token);

    void revokeAllUserTokensFromSet(UUID userId);

    Set<TokenRedisModel> getAllUserTokensSet(UUID userId);

    void setAllUserTokensSet(UUID userId, Set<TokenRedisModel> tokens);
}
