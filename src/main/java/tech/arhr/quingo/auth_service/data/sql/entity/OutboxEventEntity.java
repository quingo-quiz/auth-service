package tech.arhr.quingo.auth_service.data.sql.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import tech.arhr.quingo.auth_service.enums.EventType;
import tech.arhr.quingo.auth_service.enums.OutboxStatus;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OutboxEventEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private EventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "retry_count")
    private int retryCount;
}
