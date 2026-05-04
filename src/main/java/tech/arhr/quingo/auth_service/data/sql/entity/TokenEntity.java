package tech.arhr.quingo.auth_service.data.sql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenEntity {
    @Id
    private UUID id;

    @Column(length = 400)
    private String token;
    private Instant expiresAt;
    private Instant issuedAt;
    private boolean revoked;

    @ManyToOne
    private UserEntity user;

    @Column(length = 100)
    private String browser;
    @Column(length = 100)
    private String os;
    @Column(length = 100)
    private String device;
    @Column(length = 100, name = "ip_address")
    private String ipAddress;
}
