package tech.arhr.quingo.auth_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TokenPairDto {
    private TokenDto accessToken;
    private TokenDto refreshToken;
}
