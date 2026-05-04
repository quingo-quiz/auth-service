package tech.arhr.quingo.auth_service.services.mfa;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.JpaMfaSettingsRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.UserMfaSettingsEntity;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.auth.OtpConnectDto;
import tech.arhr.quingo.auth_service.dto.auth.OtpVerifyRequest;
import tech.arhr.quingo.auth_service.enums.MfaType;
import tech.arhr.quingo.auth_service.exceptions.auth.MfaFailedException;
import tech.arhr.quingo.auth_service.exceptions.auth.MfaSettingsInvalidException;
import tech.arhr.quingo.auth_service.services.UserService;
import tech.arhr.quingo.auth_service.utils.EncryptionUtil;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MfaService {
    private final JpaMfaSettingsRepository mfaSettingsRepository;
    private final UserService userService;
    private final OtpService otpService;
    private final EncryptionUtil encryptionUtil;

    @Transactional
    public OtpConnectDto connectOtp(UserDto user) {
        if (!userService.isPasswordSetForUser(user.getId())) {
            throw new MfaSettingsInvalidException("Please set up your password");
        }

        List<UserMfaSettingsEntity> entities = mfaSettingsRepository.findByUserIdAndType(user.getId(), MfaType.OTP);
        UserMfaSettingsEntity entity;
        if (entities.isEmpty()) {
            entity = UserMfaSettingsEntity.builder()
                    .type(MfaType.OTP)
                    .userId(user.getId())
                    .methodEnabled(false)
                    .build();
        } else {
            entity = entities.getFirst();
            if (entity.isMethodEnabled()) throw new MfaSettingsInvalidException("Method already enabled");
        }

        String secret = otpService.generateSecret();
        entity.setSecretKey(encryptionUtil.encrypt(secret));
        mfaSettingsRepository.save(entity);

        String uri = otpService.createUriForSecret(secret, user.getEmail());
        return new OtpConnectDto(uri);
    }

    @Transactional
    public void verifyConnectingOtp(UserDto user, OtpVerifyRequest request) {
        List<UserMfaSettingsEntity> entities = mfaSettingsRepository.findByUserIdAndType(user.getId(), MfaType.OTP);
        if (entities.isEmpty()) {
            throw new MfaSettingsInvalidException("2FA settings for OTP method not found");
        }
        UserMfaSettingsEntity entity = entities.getFirst();
        String encryptedSecret = entity.getSecretKey();

        if (entity.isMethodEnabled()) {
            throw new MfaSettingsInvalidException("2FA method is already enabled");
        }

        if (!otpService.verifyCode(encryptedSecret, request.getCode())) {
            throw new MfaFailedException("2FA code verification failed");
        }

        entity.setMethodEnabled(true);
        mfaSettingsRepository.save(entity);
        userService.setMfaEnabledForUser(user.getId());
    }

    @Transactional(readOnly = true)
    public void verifyOtpCode(UUID userId, OtpVerifyRequest request) {
        List<UserMfaSettingsEntity> entities = mfaSettingsRepository.findByUserIdAndType(userId, MfaType.OTP);
        if (entities.isEmpty()) {
            throw new MfaSettingsInvalidException("2FA settings for OTP method not found");
        }
        UserMfaSettingsEntity entity = entities.getFirst();
        String encriptedSecret = entity.getSecretKey();

        if (!entity.isMethodEnabled()) {
            throw new MfaSettingsInvalidException("2FA OTP method is not enabled");
        }

        if (!otpService.verifyCode(encriptedSecret, request.getCode())) {
            throw new MfaFailedException("2FA code verification failed");
        }
    }

    @Transactional(readOnly = true)
    public List<MfaType> getUserMfaTypes(UUID userId) {
        return mfaSettingsRepository.findByUserId(userId).stream()
                .map(UserMfaSettingsEntity::getType)
                .collect(Collectors.toList());
    }
}
