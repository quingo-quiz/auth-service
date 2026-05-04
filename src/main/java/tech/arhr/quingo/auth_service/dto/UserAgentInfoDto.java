package tech.arhr.quingo.auth_service.dto;

import lombok.Data;

@Data
public class UserAgentInfoDto {
    private String userAgentRaw;
    private String browser;
    private String os;
    private String device;
    private String ipAddress;
}
