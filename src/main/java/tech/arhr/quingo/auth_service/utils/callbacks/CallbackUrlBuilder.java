package tech.arhr.quingo.auth_service.utils.callbacks;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;


@Component
public class CallbackUrlBuilder {
    @Value("${spring.application.frontend-url}")
    private String frontendUrl;

    public String buildCallbackUrl(CallbackStatus status, CallbackCode code, String message){
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/callback")
                .queryParam("status", status.name())
                .queryParam("code", code.name())
                .queryParam("message", message)
                .build().toUriString();
        return targetUrl;
    }
}
