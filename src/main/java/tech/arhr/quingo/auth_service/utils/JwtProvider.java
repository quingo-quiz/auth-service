package tech.arhr.quingo.auth_service.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtProvider {
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

    private final TimeProvider timeProvider;

    @PostConstruct
    public void init() {
        ALGORITHM = Algorithm.HMAC256(JWT_SECRET);

        VERIFIER = JWT.require(ALGORITHM)
                .withIssuer(ISSUER)
                .build();
    }

    public TokenDto createAccessToken(UserDto user, UUID sessionId) {
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
                .withClaim("sid", sessionId.toString())
                .sign(ALGORITHM);

        return TokenDto.builder()
                .id(id)
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .userDto(user)
                .build();
    }

    public TokenDto createRefreshToken(UserDto user, UUID sessionId, UserAgentInfoDto agentInfo) {
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
                .withClaim("sid", sessionId.toString())
                .sign(ALGORITHM);

        return TokenDto.builder()
                .id(id)
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .userDto(user)
                .userAgentInfo(agentInfo)
                .build();

    }

    public TokenDto createMfaTempToken(UserDto user) {
        Instant issuedAt = timeProvider.now();
        Instant expiresAt = issuedAt.plusSeconds(60L * 5);
        UUID id = UUID.randomUUID();

        String token = JWT.create()
                .withSubject(user.getId().toString())
                .withIssuer(ISSUER)
                .withAudience(ISSUER)
                .withIssuedAt(issuedAt)
                .withExpiresAt(expiresAt)
                .withJWTId(id.toString())
                .withClaim("typ", "mfa_token")
                .sign(ALGORITHM);

        return TokenDto.builder()
                .id(id)
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
    }

    public UUID validateMfaTempToken(String token) {
        DecodedJWT jwt = decodeToken(token);
        if (!"mfa_token".equals(jwt.getClaim("typ").asString())) {
            throw new InvalidTokenException("Invalid token type");
        }
        return UUID.fromString(jwt.getSubject());
    }


    /**
     *
     * @param accessToken accessToken
     * @return Token ID
     */
    public UUID validateAccessToken(String accessToken) {
        DecodedJWT jwt = decodeToken(accessToken);
        String typ = jwt.getClaim("typ").asString();
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());

        if (!typ.equals("access"))
            throw new InvalidTokenException("Token is not access");
        return tokenId;
    }

    /**
     *
     * @param refreshToken refresh token
     * @return Token ID
     */
    public UUID validateRefreshToken(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID tokenId = UUID.fromString(jwt.getClaim("jti").asString());

        if (!jwt.getClaim("typ").asString().equals("refresh")) {
            throw new InvalidTokenException("Token is not refresh");
        }
        return tokenId;
    }

    public UUID getUserIdFromToken(String token){
        DecodedJWT jwt = decodeToken(token);
        return UUID.fromString(jwt.getSubject());
    }

    public UUID getSessionIdFromToken(String token){
        DecodedJWT jwt = decodeToken(token);
        return UUID.fromString(jwt.getClaim("sid").asString());
    }

    public UserDto getUserDtoFromToken(String token){
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

    public DecodedJWT decodeToken(String token) {
        try {
            return VERIFIER.verify(token);
        } catch (Exception e) {
            throw new InvalidTokenException(e.getMessage());
        }
    }
}
