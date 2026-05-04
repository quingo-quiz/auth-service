package tech.arhr.quingo.auth_service.api.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import ua_parser.Client;
import ua_parser.Parser;

@Slf4j
@Getter
public class CustomWebAuthenticationDetails extends WebAuthenticationDetails {
    private final UserAgentInfoDto userAgentInfo;

    public CustomWebAuthenticationDetails(HttpServletRequest request) {
        super(request);

        String header = request.getHeader("User-Agent");
        UserAgentInfoDto dto = new UserAgentInfoDto();
        dto.setUserAgentRaw(header);

        if (header != null) {
            Client c = new Parser().parse(header);
            dto.setOs(c.os.family);
            dto.setBrowser(c.userAgent.family);
            dto.setDevice(c.device.family);
        }
        dto.setIpAddress(request.getRemoteAddr());

        this.userAgentInfo = dto;
    }
}
