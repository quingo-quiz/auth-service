package tech.arhr.quingo.auth_service.dto;

import lombok.Data;
import tech.arhr.quingo.auth_service.enums.AccountStatus;
import tech.arhr.quingo.auth_service.enums.MfaType;
import tech.arhr.quingo.auth_service.enums.UserRole;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

import java.util.List;
import java.util.UUID;

@Data
public class SecurityStatusDto {
    private UUID userId;
    private String email;
    private boolean emailVerified;
    private boolean passwordSet;
    private boolean mfaEnabled;
    private List<MfaType> mfaTypes;
    private List<UserRole> roles;
    private List<OAuth2Provider> linkedProviders;
    private AccountStatus accountStatus;
}
