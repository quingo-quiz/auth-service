package tech.arhr.quingo.auth_service.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.data.sql.JpaTokenRepository;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import com.auth0.jwt.algorithms.Algorithm;
import tech.arhr.quingo.auth_service.exceptions.QuingoAppException;
import tech.arhr.quingo.auth_service.exceptions.auth.InvalidTokenException;

import java.time.Instant;
import java.time.OffsetDateTime;
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

    private Algorithm ALGORITHM = Algorithm.HMAC256(JWT_SECRET);

    private JWTVerifier verifier = JWT.require(ALGORITHM)
            .withIssuer(ISSUER)
            .build();

    private final JpaTokenRepository tokenRepository;
    private final UserService userService;

    {
        log.info(JWT_SECRET);
        log.info(ISSUER);
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
                .withClaim("username", user.getUsername())
                .withClaim("email", user.getEmail())
                .sign(ALGORITHM);

        return TokenDto.builder()
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .secondsAlive(ACCESS_EXPIRATION_MINUTES * 60L)
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
                .sign(ALGORITHM);
        TokenDto tokenDto = TokenDto.builder()
                .token(token)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .secondsAlive(REFRESH_EXPIRATION_DAYS * 24 * 60 * 60L)
                .build();

        tokenRepository.save(TokenDto.toEntity(tokenDto));

        return tokenDto;
    }

    public boolean validateToken(String token) {
        try {
            decodeToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UserDto getUserFromToken(String token) {
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

    public DecodedJWT decodeToken(String token) {
        try {
            return verifier.verify(token);
        } catch (Exception e) {
            throw new InvalidTokenException(e.getMessage());
        }
    }

    public void revokeRefreshToken(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        UUID tokenId = UUID.fromString(jwt.getId());
        TokenEntity tokenEntity = tokenRepository.findById(tokenId).orElse(null);
        if (tokenEntity != null) {
            tokenEntity.setRevoked(true);
        } else{
            throw new InvalidTokenException("Token not found");
        }
    }

    public void revokeAllUserTokens(String refreshToken) {
        
    }
}
