package tech.arhr.quingo.auth_service.api.security;

import lombok.Data;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;

@Component
@RequestScope(proxyMode = ScopedProxyMode.TARGET_CLASS)
@Data
public class ClientContext {
    private UserAgentInfoDto userAgentInfo;
}
