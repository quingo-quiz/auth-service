package tech.arhr.quingo.auth_service.api.security.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.api.rest.models.ErrorResponse;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.io.IOException;
import java.time.OffsetDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;
    private final TimeProvider timeProvider;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String message = "Authentication required: not found or invalid access_token cookie";
        if (authException instanceof BadCredentialsException) {
            message = authException.getMessage();
        }

        HttpStatus status = HttpStatus.UNAUTHORIZED;

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(status.value())
                .statusMessage(status.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .method(request.getMethod())
                .timestamp(timeProvider.now())
                .details(null)
                .build();

        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
