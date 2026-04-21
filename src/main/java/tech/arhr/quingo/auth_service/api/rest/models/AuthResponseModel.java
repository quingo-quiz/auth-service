package tech.arhr.quingo.auth_service.api.rest.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tech.arhr.quingo.auth_service.dto.TokenDto;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;

@Getter
@AllArgsConstructor
public class AuthResponseModel {
    private final String accessToken;
    private final String refreshToken;
    private String mfaTempToken;
    private boolean mfaRequired;

    public static AuthResponseModel from(AuthResponse authResponse) {
        if (authResponse.isMfaRequired()) {
            return new AuthResponseModel(
                    null,
                    null,
                    authResponse.getMfaTempToken().getToken(),
                    true);
        } else {
            return new AuthResponseModel(
                    authResponse.getAccessToken().getToken(),
                    authResponse.getRefreshToken().getToken(),
                    null,
                    false);
        }
    }
}
