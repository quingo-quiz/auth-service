package tech.arhr.quingo.auth_service.services.mfa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tech.arhr.quingo.auth_service.data.sql.JpaMfaSettingsRepository;
import tech.arhr.quingo.auth_service.data.sql.JpaUserRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.data.sql.entity.UserMfaSettingsEntity;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.OtpConnectDto;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.enums.MfaType;
import tech.arhr.quingo.auth_service.exceptions.auth.MfaFailedException;
import tech.arhr.quingo.auth_service.exceptions.auth.MfaSettingsInvalidException;
import tech.arhr.quingo.auth_service.utils.EncryptionUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private JpaMfaSettingsRepository mfaSettingsRepository;

    @Mock
    private JpaUserRepository userRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private EncryptionUtil encryptionUtil;

    private MfaService mfaService;

    @BeforeEach
    void setUp() {
        mfaService = new MfaService(mfaSettingsRepository, userRepository, otpService, encryptionUtil);
    }

    @Test
    void connectOtp_ReplacesExistingSettingsAndReturnsUri() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).email("user@test.com").build();

        when(otpService.generateSecret()).thenReturn("plain-secret");
        when(otpService.createUriForSecret("plain-secret", "user@test.com")).thenReturn("otpauth://uri");
        when(encryptionUtil.encrypt("plain-secret")).thenReturn("encrypted-secret");

        OtpConnectDto response = mfaService.connectOtp(user);

        assertThat(response.getSecretUri()).isEqualTo("otpauth://uri");
        verify(mfaSettingsRepository).deleteByUserIdEqualsAndType(userId, MfaType.OTP);

        ArgumentCaptor<UserMfaSettingsEntity> captor = ArgumentCaptor.forClass(UserMfaSettingsEntity.class);
        verify(mfaSettingsRepository).save(captor.capture());
        UserMfaSettingsEntity saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getType()).isEqualTo(MfaType.OTP);
        assertThat(saved.getSecretKey()).isEqualTo("encrypted-secret");
        assertThat(saved.isMethodEnabled()).isFalse();
    }

    @Test
    void verifyConnectingOtp_NoSettings_ThrowsMfaSettingsInvalidException() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).build();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("123456");

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of());

        assertThatThrownBy(() -> mfaService.verifyConnectingOtp(user, request))
                .isInstanceOf(MfaSettingsInvalidException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void verifyConnectingOtp_AlreadyEnabled_ThrowsMfaSettingsInvalidException() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).build();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("123456");

        UserMfaSettingsEntity settings = UserMfaSettingsEntity.builder()
                .userId(userId)
                .type(MfaType.OTP)
                .secretKey("encrypted")
                .methodEnabled(true)
                .build();

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of(settings));

        assertThatThrownBy(() -> mfaService.verifyConnectingOtp(user, request))
                .isInstanceOf(MfaSettingsInvalidException.class)
                .hasMessageContaining("already enabled");
    }

    @Test
    void verifyConnectingOtp_InvalidCode_ThrowsMfaFailedException() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).build();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("000000");

        UserMfaSettingsEntity settings = UserMfaSettingsEntity.builder()
                .userId(userId)
                .type(MfaType.OTP)
                .secretKey("encrypted")
                .methodEnabled(false)
                .build();

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of(settings));
        when(otpService.verifyCode("encrypted", "000000")).thenReturn(false);

        assertThatThrownBy(() -> mfaService.verifyConnectingOtp(user, request))
                .isInstanceOf(MfaFailedException.class)
                .hasMessageContaining("verification failed");
    }

    @Test
    void verifyConnectingOtp_ValidCode_EnablesMethodAndUserMfa() {
        UUID userId = UUID.randomUUID();
        UserDto user = UserDto.builder().id(userId).build();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("654321");

        UserMfaSettingsEntity settings = UserMfaSettingsEntity.builder()
                .userId(userId)
                .type(MfaType.OTP)
                .secretKey("encrypted")
                .methodEnabled(false)
                .build();
        UserEntity userEntity = UserEntity.builder().id(userId).mfaEnabled(false).build();

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of(settings));
        when(otpService.verifyCode("encrypted", "654321")).thenReturn(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(userEntity));

        mfaService.verifyConnectingOtp(user, request);

        assertThat(settings.isMethodEnabled()).isTrue();
        assertThat(userEntity.isMfaEnabled()).isTrue();
        verify(mfaSettingsRepository).save(settings);
        verify(userRepository).save(userEntity);
    }

    @Test
    void verifyOtpCode_MethodDisabled_ThrowsMfaSettingsInvalidException() {
        UUID userId = UUID.randomUUID();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("123456");

        UserMfaSettingsEntity settings = UserMfaSettingsEntity.builder()
                .userId(userId)
                .type(MfaType.OTP)
                .secretKey("encrypted")
                .methodEnabled(false)
                .build();

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of(settings));

        assertThatThrownBy(() -> mfaService.verifyOtpCode(userId, request))
                .isInstanceOf(MfaSettingsInvalidException.class)
                .hasMessageContaining("not enabled");
    }

    @Test
    void verifyOtpCode_ValidCode_NoException() {
        UUID userId = UUID.randomUUID();
        OtpVerifyRequest request = new OtpVerifyRequest();
        request.setCode("123456");

        UserMfaSettingsEntity settings = UserMfaSettingsEntity.builder()
                .userId(userId)
                .type(MfaType.OTP)
                .secretKey("encrypted")
                .methodEnabled(true)
                .build();

        when(mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP)).thenReturn(List.of(settings));
        when(otpService.verifyCode("encrypted", "123456")).thenReturn(true);

        mfaService.verifyOtpCode(userId, request);

        verify(otpService).verifyCode("encrypted", "123456");
    }
}
