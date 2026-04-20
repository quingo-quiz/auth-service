package tech.arhr.quingo.auth_service.data.sql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.services.oauth2.OAuth2Provider;

import java.util.UUID;

@Entity
@Data
@Table(name = "user_social_account")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SocialAccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    private OAuth2Provider provider;

    @Column(name = "provider_user_id")
    private String providerUserId;
}
