package tech.arhr.quingo.auth_service.api.rest.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class RequestsLogFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        log.info("=== New Request ===");

        log.info("IP: {}", request.getRemoteAddr());
        log.info("UserAgent: {}", request.getHeader("User-Agent"));
        log.info("Path: {}", request.getContextPath());
        request.getHeaderNames().asIterator().forEachRemaining(key -> {
            log.info("header: {}", key);
        });

        log.info("===================");

        filterChain.doFilter(request, response);
    }
}
