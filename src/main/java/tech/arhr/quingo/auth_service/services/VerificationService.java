package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.VerificationTokenDto;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;
import tech.arhr.quingo.auth_service.exceptions.auth.TokenNotFoundException;
import tech.arhr.quingo.auth_service.utils.VerificationTokenMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final RedisVerificationTokenRepository redisRepository;
    private final OutboxService outboxService;
    private final VerificationTokenMapper mapper;

    private VerificationTokenDto generateVerificationToken(UUID userId,  VerificationTokenType type) {
        var token = new VerificationTokenDto();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setType(type);

        VerificationTokenRedisModel model = mapper.toModel(token);
        redisRepository.save(model);
        return token;
    }

    public void sendVerificationEmail(UserDto userDto) {
        VerificationTokenDto token = generateVerificationToken(userDto.getId(), VerificationTokenType.VERIFY_EMAIL);
        outboxService.sendVerifyEmailEvent(userDto, token.getToken());
    }

    public void sendResetPasswordEmail(UserDto userDto) {
        VerificationTokenDto token = generateVerificationToken(userDto.getId(), VerificationTokenType.VERIFY_EMAIL);
        outboxService.sendVerifyEmailEvent(userDto, token.getToken());
    }

    public UUID getUserIdIfTokenExists(String token, VerificationTokenType type) {
        Optional<VerificationTokenRedisModel> opt = redisRepository.get(token, type);
        if (opt.isPresent()) {
            return opt.get().getUserId();
        } else {
            throw new TokenNotFoundException();
        }
    }

}
