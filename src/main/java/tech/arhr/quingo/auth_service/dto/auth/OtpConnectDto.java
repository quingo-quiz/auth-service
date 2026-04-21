package tech.arhr.quingo.auth_service.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OtpConnectDto {
    private String secretUri;
}
