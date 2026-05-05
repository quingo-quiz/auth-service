package tech.arhr.quingo.auth_service.data.kafka.events.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ResetPasswordPayload {
    private String email;
    private String verificationToken;
}
