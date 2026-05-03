package tech.arhr.quingo.auth_service.api.security;

import lombok.Data;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;

@Component
@RequestScope
@Data
public class ClientContext {
    private UserAgentInfoDto userAgentInfo;
}
