package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.dto.SecurityStatusDto;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.services.mfa.MfaService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SecurityService {
    private final UserService userService;
    private final MfaService mfaService;
    private final SocialAccountService socialAccountService;

    public SecurityStatusDto getUserSecurityStatus(UUID userId) {
        SecurityStatusDto status = new SecurityStatusDto();
        UserDto user = userService.getUserById(userId);

        status.setUserId(user.getId());
        status.setEmail(user.getEmail());
        status.setEmailVerified(user.isEmailVerified());
        status.setPasswordSet(userService.isPasswordSetForUser(userId));
        status.setMfaEnabled(user.isMfaEnabled());
        status.setRoles(user.getRoles());
        status.setAccountStatus(user.getAccountStatus());
        status.setMfaTypes(mfaService.getUserMfaTypes(userId));
        status.setLinkedProviders(socialAccountService.getUserLinkedProviders(userId));

        return status;
    }
}
