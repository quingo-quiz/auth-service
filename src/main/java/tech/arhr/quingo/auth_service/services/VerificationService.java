package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.VerificationTokenDto;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;
import tech.arhr.quingo.auth_service.events.user.UserEmailVerifiedEvent;
import tech.arhr.quingo.auth_service.events.user.UserPasswordResetEvent;
import tech.arhr.quingo.auth_service.events.user.UserRegisteredEvent;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyVerifiedException;
import tech.arhr.quingo.auth_service.exceptions.auth.TokenNotFoundException;
import tech.arhr.quingo.auth_service.utils.VerificationTokenMapper;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final RedisVerificationTokenRepository redisRepository;
    private final OutboxService outboxService;
    private final ApplicationEventPublisher publisher;
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

    @Transactional
    public void sendVerificationEmail(UserDto userDto) {
        if (userDto.isEmailVerified())
            throw new EmailAlreadyVerifiedException();

        VerificationTokenDto token = generateVerificationToken(userDto.getId(), VerificationTokenType.VERIFY_EMAIL);
        outboxService.sendVerifyEmailEvent(userDto, token.getToken());
    }

    @Transactional
    public void sendResetPasswordEmail(String email, UUID userId) {
        VerificationTokenDto token = generateVerificationToken(userId, VerificationTokenType.RESET_PASSWORD);
        outboxService.sendResetPasswordEvent(email, token.getToken());
    }

    public void verifyEmailVerification(String token) {
        var type = VerificationTokenType.VERIFY_EMAIL;
        Optional<VerificationTokenRedisModel> opt = redisRepository.get(token, type);
        redisRepository.delete(token, type);
        if (opt.isEmpty())
            throw new TokenNotFoundException("Verification token is invalid");

        publisher.publishEvent(new UserEmailVerifiedEvent(opt.get().getUserId()));
    }

    public void verifyResetPassword(String token, String newPassword) {
        var type = VerificationTokenType.RESET_PASSWORD;
        Optional<VerificationTokenRedisModel> opt = redisRepository.get(token, type);
        redisRepository.delete(token, type);
        if (opt.isEmpty())
            throw new TokenNotFoundException("Verification token is invalid");

        publisher.publishEvent(new UserPasswordResetEvent(opt.get().getUserId(), newPassword));
    }

    @EventListener(UserRegisteredEvent.class)
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        sendVerificationEmail(event.user());
    }
}
