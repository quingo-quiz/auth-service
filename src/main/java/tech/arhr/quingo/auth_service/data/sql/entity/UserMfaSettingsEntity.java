package tech.arhr.quingo.auth_service.data.sql.entity;

import jakarta.persistence.*;
import lombok.*;
import tech.arhr.quingo.auth_service.enums.MfaType;

import java.util.UUID;

@Entity
@Table(name = "user_mfa_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMfaSettingsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID userId;

    private MfaType type;

    @Column(name = "secret_key")
    private String secretKey;

    @Column(name = "method_enabled")
    private boolean methodEnabled;

}
