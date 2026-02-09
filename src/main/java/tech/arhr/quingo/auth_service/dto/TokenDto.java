package tech.arhr.quingo.auth_service.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@NoArgsConstructor
public class TokenDto {
    private String token;
}
