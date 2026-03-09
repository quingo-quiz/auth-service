package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.redis.RedisTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhiteListTokenService {
    private final RedisTokenRepository redisTokenRepository;
    private final TokenMapper tokenMapper;
    private final TimeProvider timeProvider;

    public void registerToken(TokenDto token) {
        TokenRedisModel model = tokenMapper.toRedisModel(token);
        UUID userId = model.getUserId();

        redisTokenRepository.addToUserTokensSet(userId, model);
        redisTokenRepository.save(model);
        deleteExpiredTokensFromSet(userId);
    }

    public boolean isBlocked(UUID tokenId) {
        return !redisTokenRepository.exists(tokenId);
    }

    public void blockToken(UUID tokenId) {
        redisTokenRepository.delete(tokenId);
    }

    public void blockAllUserTokens(UUID userId) {
        redisTokenRepository.revokeAllUserTokensFromSet(userId);
    }

    private void deleteExpiredTokensFromSet(UUID userId) {
        Set<TokenRedisModel> tokens = redisTokenRepository.getAllUserTokensSet(userId);

        Set<TokenRedisModel> activeTokens = tokens.stream().
                filter(token ->
                        token.getExpireTime().isAfter(timeProvider.now()))
                .collect(Collectors.toSet());

        redisTokenRepository.setAllUserTokensSet(userId, activeTokens);
    }
}
