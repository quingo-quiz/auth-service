package tech.arhr.quingo.auth_service.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UserProfileDto {
    private UUID id;
    private String username;
    private String bio;
}
