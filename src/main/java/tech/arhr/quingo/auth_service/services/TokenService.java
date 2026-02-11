package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import com.auth0.jwt.algorithms.Algorithm;
import tech.arhr.quingo.auth_service.exceptions.auth.AuthException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;
import tech.arhr.quingo.auth_service.utils.Hasher;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log
public class TokenService {
    @Value("${spring.jwt.secret}")
    private String JWT_SECRET = "sd";

    @Value("${spring.application.domain}")
    private String ISSUER;

    @Value("${spring.jwt.expiration.access-minutes}")
    private int ACCESS_EXPIRATION_MINUTES;

    @Value("${spring.jwt.expiration.refresh-days}")
    private int REFRESH_EXPIRATION_DAYS;

    private Algorithm ALGORITHM;

    private JWTVerifier VERIFIER;

    private final JpaTokenRepository tokenRepository;
    private final UserService userService;

    @PostConstruct
    public void init() {
        ALGORITHM = Algorithm.HMAC256(JWT_SECRET);

        VERIFIER = JWT.require(ALGORITHM)
                .withIssuer(ISSUER)
                .build();
    }


    public TokenDto createAccessToken(UserDto user) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusMinutes(ACCESS_EXPIRATION_MINUTES);
        UUID id = UUID.randomUUID();

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withIssuedAt(issuedAt.toInstant())
                .withExpiresAt(expiresAt.toInstant())
                .withJWTId(id.toString())
                .withClaim("typ", "access")
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .sign(ALGORITHM);

        return TokenDto.builder()
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    public TokenDto createRefreshToken(UserDto user) {
        OffsetDateTime issuedAt = OffsetDateTime.now();
        OffsetDateTime expiresAt = issuedAt.plusDays(REFRESH_EXPIRATION_DAYS);
        UUID id = UUID.randomUUID();

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withIssuedAt(issuedAt.toInstant())
                .withExpiresAt(expiresAt.toInstant())
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

        TokenEntity tokenEntity = TokenDto.toEntity(tokenDto);
        tokenEntity.setToken(Hasher.hash(tokenEntity.getToken()));
        tokenRepository.save(tokenEntity);

        return tokenDto;
    }

    public void validateAccessToken(String accessToken) {
        DecodedJWT jwt = decodeToken(accessToken);
        if (!jwt.getClaim("typ").asString().equals("access")) {
            throw new InvalidTokenException("Token is not access");
        }
    }

    public void validateRefreshToken(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);

        if (!jwt.getClaim("typ").asString().equals("refresh")) {
            throw new InvalidTokenException("Token is not refresh");
        }
        if (isRefreshTokenRevoked(refreshToken)) {
            throw new InvalidTokenException("Token is revoked");
        }
    }

    public UserDto getUserFromTokenNoQuery(String token) {
        DecodedJWT jwt = decodeToken(token);
        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaim("username").asString();
        String email = jwt.getClaim("email").asString();
        return UserDto.builder()
                .id(userId)
                .username(username)
                .email(email)
                .build();
    }

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

    public void revokeAllUserTokens(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID userId = UUID.fromString(jwt.getSubject());
        List<TokenEntity> entities = tokenRepository.findAllByUserId(userId);
        entities.forEach(tokenEntity -> {tokenEntity.setRevoked(true);});
    }

    private boolean isRefreshTokenRevoked(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());
        TokenEntity entity = tokenRepository.findById(tokenId).orElseThrow(() -> new InvalidTokenException("Token not found"));
        return entity.isRevoked();
    }
}
