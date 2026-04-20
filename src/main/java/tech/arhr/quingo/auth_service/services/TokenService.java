package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.Hasher;
import tech.arhr.quingo.auth_service.utils.TimeProvider;
import tech.arhr.quingo.auth_service.utils.TokenMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {
    @Value("${spring.jwt.secret}")
    private String JWT_SECRET;

    @Value("${spring.application.domain}")
    private String ISSUER;

    @Value("${spring.jwt.expiration.access-minutes}")
    private int ACCESS_EXPIRATION_MINUTES;

    @Value("${spring.jwt.expiration.refresh-days}")
    private int REFRESH_EXPIRATION_DAYS;

    private Algorithm ALGORITHM;

    private JWTVerifier VERIFIER;

    private final JpaTokenRepository tokenRepository;
    private final WhiteListTokenService whiteListTokenService;
    private final UserService userService;
    private final Hasher hasher;
    private final TokenMapper tokenMapper;
    private final TimeProvider timeProvider;

    @PostConstruct
    public void init() {
        ALGORITHM = Algorithm.HMAC256(JWT_SECRET);

        VERIFIER = JWT.require(ALGORITHM)
                .withIssuer(ISSUER)
                .build();
    }

    @Transactional
    public TokenDto createAccessToken(UserDto user) {
        Instant issuedAt = timeProvider.now();
        Instant expiresAt = issuedAt.plusSeconds(60L * ACCESS_EXPIRATION_MINUTES);
        UUID id = UUID.randomUUID();

        if (user.getRoles() == null || user.getRoles().isEmpty())
            throw new InvalidTokenException("Invalid user roles");
        List<String> roles = user.getRoles().stream()
                .map(UserRole::toString).toList();

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .withJWTId(id.toString())
                .withClaim("typ", "access")
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .withClaim("emailVerified", user.isEmailVerified())
                .withClaim("roles", roles)
                .sign(ALGORITHM);

        TokenDto dto = TokenDto.builder()
                .id(id)
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .userDto(user)
                .build();

        whiteListTokenService.registerToken(dto);
        return dto;
    }

    @Transactional
    public TokenDto createRefreshToken(UserDto user) {
        Instant issuedAt = timeProvider.now();
        Instant expiresAt = issuedAt.plusSeconds(60L * 60 * 24 * REFRESH_EXPIRATION_DAYS);
        UUID id = UUID.randomUUID();

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .withJWTId(id.toString())
                .withClaim("typ", "refresh")
                .sign(ALGORITHM);

        TokenDto tokenDto = TokenDto.builder()
                .id(id)
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .userDto(user)
                .build();

        TokenEntity tokenEntity = tokenMapper.toEntity(tokenDto);
        tokenEntity.setToken(hasher.hash(tokenEntity.getToken()));
        tokenRepository.save(tokenEntity);
        return tokenDto;
    }

    public void validateAccessToken(String accessToken) {
        DecodedJWT jwt = decodeToken(accessToken);
        UUID jti = UUID.fromString(jwt.getClaim("jti").asString());
        String typ = jwt.getClaim("typ").asString();

        if (whiteListTokenService.isBlocked(jti))
            throw new InvalidTokenException("Token is blocked");

        if (!typ.equals("access"))
            throw new InvalidTokenException("Token is not access");

    }

    public void validateRefreshToken(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());

        if (!jwt.getClaim("typ").asString().equals("refresh")) {
            throw new InvalidTokenException("Token is not refresh");
        }

        TokenEntity entity = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new InvalidTokenException("Token not found"));

        if (entity.isRevoked()) {
            throw new InvalidTokenException("Token is revoked");
        }

        if (!hasher.verify(refreshToken, entity.getToken()))
            throw new InvalidTokenException("Token not exists");
    }

    public UserDto getUserFromTokenNoQuery(String token) {
        DecodedJWT jwt = decodeToken(token);
        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaim("username").asString();
        String email = jwt.getClaim("email").asString();
        boolean emailVerified = jwt.getClaim("emailVerified").asBoolean();
        List<UserRole> roles = jwt.getClaim("roles").asList(UserRole.class);
        return UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .emailVerified(emailVerified)
                .roles(roles)
                .build();
    }

    @Transactional(readOnly = true)
    public UserDto getUserFromTokenWithQuery(String token) {
        DecodedJWT jwt = decodeToken(token);
        UUID userId = UUID.fromString(jwt.getSubject());
        return userService.getUserById(userId);
    }


    public DecodedJWT decodeToken(String token) {
        try {
            return VERIFIER.verify(token);
        } catch (Exception e) {
            throw new InvalidTokenException(e.getMessage());
        }
    }

    @Transactional
    public void revokeRefreshToken(String refreshToken) {
        validateRefreshToken(refreshToken);
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());
        TokenEntity tokenEntity = tokenRepository.findById(tokenId).orElse(null);
        if (tokenEntity != null) {
            tokenEntity.setRevoked(true);
        } else {
            throw new InvalidTokenException("Token not found");
        }
    }

    public void blockAccessToken(String token) {
        validateAccessToken(token);
        DecodedJWT jwt = decodeToken(token);
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());
        whiteListTokenService.blockToken(tokenId);
    }

    @Transactional
    public void revokeAllUserTokens(String refreshToken) {
        validateRefreshToken(refreshToken);
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID userId = UUID.fromString(jwt.getSubject());

        revokeAllUserTokens(userId);
    }

    @Transactional
    public void revokeAllUserTokens(UUID userId){
        List<TokenEntity> entities = tokenRepository.findAllByUserId(userId);
        entities.forEach(tokenEntity -> tokenEntity.setRevoked(true));
        whiteListTokenService.blockAllUserTokens(userId);
    }

    public void refreshSessions(UUID userId) {
        whiteListTokenService.blockAllUserTokens(userId);
    }

    public List<TokenDto> getActiveRefreshTokens(UUID userId) {
        return tokenRepository.findAllByUserIdAndRevokedAndExpiresAtAfter(userId, false, timeProvider.now())
                .stream()
                .map(tokenMapper::toDto)
                .toList();
    }

}
