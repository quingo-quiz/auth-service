package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.SessionTokens;
import tech.arhr.quingo.auth_service.events.AllUserSessionsInvalidatedEvent;
import tech.arhr.quingo.auth_service.events.user.UserRolesChangedEvent;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.exceptions.auth.PermissionDeniedException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.JwtProvider;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    private final JpaTokenRepository tokenRepository;
    private final WhiteListTokenService whiteListTokenService;
    private final Hasher hasher;
    private final TokenMapper tokenMapper;
    private final TimeProvider timeProvider;
    private final JwtProvider jwtProvider;


    public SessionTokens createSession(UserDto user, UserAgentInfoDto agentInfo) {
        UUID sessionId = UUID.randomUUID();
        TokenDto refresh = jwtProvider.createRefreshToken(user, sessionId, agentInfo);
        TokenDto access = jwtProvider.createAccessToken(user, sessionId);

        whiteListTokenService.registerToken(access);

        TokenEntity tokenEntity = tokenMapper.toEntity(refresh);
        tokenEntity.setToken(hasher.hash(tokenEntity.getToken()));


        if (agentInfo != null) {
            tokenEntity.setBrowser(agentInfo.getBrowser());
            tokenEntity.setOs(agentInfo.getOs());
            tokenEntity.setDevice(agentInfo.getDevice());
            tokenEntity.setIpAddress(agentInfo.getIpAddress());
        }
        tokenEntity.setSessionId(sessionId);

        tokenRepository.save(tokenEntity);

        return new SessionTokens(
                access,
                refresh
        );
    }

    /**
     * @param accessToken accessToken
     * @return Token ID
     */
    public UUID validateAccessToken(String accessToken) {
        UUID tokenId = jwtProvider.validateAccessToken(accessToken);

        if (whiteListTokenService.isBlocked(tokenId))
            throw new InvalidTokenException("Token is blocked");

        return tokenId;
    }

    /**
     * @param refreshToken refresh token
     * @return Token ID
     */
    public UUID validateRefreshToken(String refreshToken) {
        UUID tokenId = jwtProvider.validateRefreshToken(refreshToken);

        TokenEntity entity = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        if (entity.isRevoked()) {
            throw new InvalidTokenException("Token is revoked");
        }

        if (!hasher.verify(refreshToken, entity.getToken()))
            throw new InvalidTokenException("Token not exists");

        return tokenId;
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        UUID tokenId = validateRefreshToken(refreshToken);

        TokenEntity tokenEntity = tokenRepository.findById(tokenId).orElse(null);
        if (tokenEntity != null) {
            tokenEntity.setRevoked(true);
        } else {
            throw new InvalidTokenException("Token not found");
        }
    }

    @Transactional
    public void revokeRefreshTokenById(String refreshToken, UUID tokenId) {
        UUID userTokenId = validateRefreshToken(refreshToken);
        TokenEntity userTokenEntity = tokenRepository.findById(userTokenId).orElse(null);
        TokenEntity tokenEntity = tokenRepository.findById(tokenId).orElse(null);

        if (!tokenEntity.getUser().getId().equals(userTokenEntity.getUser().getId())) {
            throw new PermissionDeniedException("You are not allowed to revoke this session");
        }

        tokenEntity.setRevoked(true);
    }

    public void blockAccessToken(String token) {
        UUID tokenId = validateAccessToken(token);
        whiteListTokenService.blockToken(tokenId);
    }

    @Transactional
    public void revokeAllUserTokens(String refreshToken) {
        validateRefreshToken(refreshToken);
        UUID userId = jwtProvider.getUserIdFromToken(refreshToken);
        revokeAllUserTokens(userId);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        List<TokenEntity> entities = tokenRepository.findAllByUserId(userId);
        entities.forEach(tokenEntity -> tokenEntity.setRevoked(true));
        whiteListTokenService.blockAllUserTokens(userId);
    }

    @Transactional(readOnly = true)
    public List<TokenDto> getActiveRefreshTokens(UUID userId) {
        return tokenRepository.findAllByUserIdAndRevokedAndExpiresAtAfter(userId, false, timeProvider.now())
                .stream()
                .map(tokenMapper::toDto)
                .toList();
    }

    @EventListener(AllUserSessionsInvalidatedEvent.class)
    @Transactional
    public void onInvalidateAllUserSessions(AllUserSessionsInvalidatedEvent event) {
        revokeAllUserTokens(event.userId());
    }

    @EventListener(UserRolesChangedEvent.class)
    public void onRevokeAllUserAccessTokens(UserRolesChangedEvent event) {
        whiteListTokenService.blockAllUserTokens(event.userId());
    }
}