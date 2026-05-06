package tech.arhr.quingo.auth_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import tech.arhr.quingo.auth_service.dto.TokenDto;

@Data
@AllArgsConstructor
public class SessionTokens {
    private TokenDto accessToken;
    private TokenDto refreshToken;
}
