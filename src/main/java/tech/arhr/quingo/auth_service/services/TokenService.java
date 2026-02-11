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
        try {
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
                    .sign(ALGORITHM);

            return TokenDto.builder()
                    .token(token)
                    .issuedAt(issuedAt)
                    .expiresAt(expiresAt)
                    .secondsAlive(ACCESS_EXPIRATION_MINUTES * 60L)
                    .build();
        } catch (Exception e) {
            throw new QuingoAppException(e.getMessage());
        }
    }

    public TokenDto createRefreshToken(UserDto user) {
        try {
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
        } catch (Exception e) {
            throw new QuingoAppException(e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            verifier.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UserDto getUserFromToken(String token) {
        DecodedJWT jwt = decodeToken(token);
        UUID userId = UUID.fromString(jwt.getSubject());
        return userService.getUserById(userId);
    }

    public DecodedJWT decodeToken(String token) {
        return verifier.verify(token);
    }

    public void revokeRefreshToken(String refreshToken) {
        DecodedJWT jwt = decodeToken(refreshToken);
        //TokenEntity tokenEntity = tokenRepository.findById(UUID.fromString(jwt.getId()));
    }

    public void revokeAllUserTokens(String refreshToken) {
    }
}
