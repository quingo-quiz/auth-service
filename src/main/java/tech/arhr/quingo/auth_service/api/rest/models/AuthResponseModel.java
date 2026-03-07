package tech.arhr.quingo.auth_service.api.rest.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import tech.arhr.quingo.auth_service.dto.auth.AuthResponse;

@Getter
@AllArgsConstructor
public class AuthResponseModel {
    private final String accessToken;
    private final String refreshToken;

    public static AuthResponseModel from(AuthResponse authResponse) {
        return new AuthResponseModel(authResponse.getAccessToken().getToken(),
                authResponse.getRefreshToken().getToken());
    }
}
