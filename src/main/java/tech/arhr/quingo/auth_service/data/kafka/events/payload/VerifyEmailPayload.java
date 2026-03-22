package tech.arhr.quingo.auth_service.data.kafka.events.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.dto.UserDto;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerifyEmailPayload {
    public VerifyEmailPayload(UserDto userDto, String verificationToken) {
        this.userId = userDto.getId();
        this.email = userDto.getEmail();
        this.username = userDto.getUsername();
        this.verificationToken = verificationToken;
    }

    private UUID userId;
    private String email;
    private String username;
    private String verificationToken;
}
