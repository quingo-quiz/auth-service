package tech.arhr.quingo.auth_service.data.kafka.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tech.arhr.quingo.auth_service.enums.EventType;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BaseKafkaEvent<T> {
    private UUID eventId;
    private EventType type;
    private Instant timestamp;
    private T data;
}
