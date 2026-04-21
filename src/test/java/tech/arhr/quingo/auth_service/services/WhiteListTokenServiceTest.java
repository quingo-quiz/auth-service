package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WhiteListTokenServiceTest {

    @Mock
    private RedisTokenRepository redisTokenRepository;

    @Mock
    private TokenMapper tokenMapper;

    @Mock
    private TimeProvider timeProvider;

    private WhiteListTokenService whiteListTokenService;

    @BeforeEach
    void setUp() {
        whiteListTokenService = new WhiteListTokenService(redisTokenRepository, tokenMapper, timeProvider);
    }

    @Test
    void registerToken_RemovesExpiredTokensFromUserSet() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-04-22T10:00:00Z");

        TokenDto token = TokenDto.builder().id(UUID.randomUUID()).build();

        TokenRedisModel currentToken = model(UUID.randomUUID(), userId, now.plusSeconds(3600));
        TokenRedisModel activeToken = model(UUID.randomUUID(), userId, now.plusSeconds(600));
        TokenRedisModel expiredToken = model(UUID.randomUUID(), userId, now.minusSeconds(5));

        when(tokenMapper.toRedisModel(token)).thenReturn(currentToken);
        when(redisTokenRepository.getAllUserTokensSet(userId)).thenReturn(Set.of(activeToken, expiredToken));
        when(timeProvider.now()).thenReturn(now);

        whiteListTokenService.registerToken(token);

        verify(redisTokenRepository).addToUserTokensSet(userId, currentToken);
        verify(redisTokenRepository).save(currentToken);

        ArgumentCaptor<Set<TokenRedisModel>> captor = ArgumentCaptor.forClass(Set.class);
        verify(redisTokenRepository).setAllUserTokensSet(eq(userId), captor.capture());
        Set<TokenRedisModel> resultSet = captor.getValue();
        assertThat(resultSet).containsExactly(activeToken);
    }

    @Test
    void isBlocked_WhenTokenExists_ReturnsFalse() {
        UUID tokenId = UUID.randomUUID();
        when(redisTokenRepository.exists(tokenId)).thenReturn(true);

        boolean blocked = whiteListTokenService.isBlocked(tokenId);

        assertThat(blocked).isFalse();
    }

    @Test
    void blockToken_DelegatesToRedisRepository() {
        UUID tokenId = UUID.randomUUID();

        whiteListTokenService.blockToken(tokenId);

        verify(redisTokenRepository).delete(tokenId);
    }

    @Test
    void blockAllUserTokens_DelegatesToRedisRepository() {
        UUID userId = UUID.randomUUID();

        whiteListTokenService.blockAllUserTokens(userId);

        verify(redisTokenRepository).revokeAllUserTokensFromSet(userId);
    }

    private static TokenRedisModel model(UUID tokenId, UUID userId, Instant expireTime) {
        TokenRedisModel model = new TokenRedisModel();
        model.setTokenId(tokenId);
        model.setUserId(userId);
        model.setExpireTime(expireTime);
        return model;
    }
}
