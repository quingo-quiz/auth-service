package tech.arhr.quingo.auth_service.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {
    @Size(min = 1, max = 50, message = "Username must be between 1 and 50 characters")
    private String username;

    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;
}