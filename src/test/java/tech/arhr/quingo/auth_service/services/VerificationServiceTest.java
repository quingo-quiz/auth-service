package tech.arhr.quingo.auth_service.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.VerificationTokenDto;
import tech.arhr.quingo.auth_service.enums.VerificationTokenType;
import tech.arhr.quingo.auth_service.events.user.UserEmailVerifiedEvent;
import tech.arhr.quingo.auth_service.exceptions.auth.EmailAlreadyVerifiedException;
import tech.arhr.quingo.auth_service.exceptions.auth.TokenNotFoundException;
import tech.arhr.quingo.auth_service.utils.VerificationTokenMapper;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private RedisVerificationTokenRepository redisRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private org.springframework.context.ApplicationEventPublisher publisher;

    @Mock
    private VerificationTokenMapper mapper;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(redisRepository, outboxService, publisher, mapper);
    }

    @Test
    void sendVerificationEmail_WhenAlreadyVerified_Throws() {
        UserDto user = UserDto.builder().id(UUID.randomUUID()).emailVerified(true).build();

        assertThatThrownBy(() -> verificationService.sendVerificationEmail(user))
                .isInstanceOf(EmailAlreadyVerifiedException.class);
    }

    @Test
    void verifyEmailVerification_TokenMissing_Throws() {
        String token = "t";
        when(redisRepository.get(token, VerificationTokenType.VERIFY_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> verificationService.verifyEmailVerification(token))
                .isInstanceOf(TokenNotFoundException.class);

        verify(redisRepository).delete(token, VerificationTokenType.VERIFY_EMAIL);
    }

    @Test
    void verifyEmailVerification_Success_PublishesEvent() {
        String token = "t";
        VerificationTokenRedisModel model = new VerificationTokenRedisModel();
        UUID userId = UUID.randomUUID();
        model.setUserId(userId);

        when(redisRepository.get(token, VerificationTokenType.VERIFY_EMAIL)).thenReturn(Optional.of(model));

        verificationService.verifyEmailVerification(token);

        verify(redisRepository).delete(token, VerificationTokenType.VERIFY_EMAIL);
        verify(publisher).publishEvent(any(UserEmailVerifiedEvent.class));
    }
}
