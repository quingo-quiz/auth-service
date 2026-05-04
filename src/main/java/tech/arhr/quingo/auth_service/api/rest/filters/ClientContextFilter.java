package tech.arhr.quingo.auth_service.api.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tech.arhr.quingo.auth_service.api.security.ClientContext;
import tech.arhr.quingo.auth_service.dto.UserAgentInfoDto;
import ua_parser.Client;
import ua_parser.Parser;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientContextFilter extends OncePerRequestFilter {
    private final ClientContext context;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
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
        context.setUserAgentInfo(dto);

        filterChain.doFilter(request, response);
    }
}
