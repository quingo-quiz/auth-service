package tech.arhr.quingo.auth_service.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.kafka.events.BaseKafkaEvent;
import tech.arhr.quingo.auth_service.data.kafka.events.payload.VerifyEmailPayload;
import tech.arhr.quingo.auth_service.data.sql.JpaOutboxRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.OutboxEventEntity;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.enums.EventType;
import tech.arhr.quingo.auth_service.enums.OutboxStatus;
import tech.arhr.quingo.auth_service.utils.TimeProvider;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {
    private final JpaOutboxRepository outboxRepository;
    private final TimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.MANDATORY)
    public void sendVerifyEmailEvent(UserDto userDto, String verificationToken) {
        VerifyEmailPayload payloadData = new VerifyEmailPayload(userDto, verificationToken);
        saveEvent(payloadData, EventType.VERIFY_EMAIL);
    }


    private <T> void saveEvent(T data, EventType type) {
        Instant now = timeProvider.now();
        UUID eventId = UUID.randomUUID();

        BaseKafkaEvent<T> fullEvent = BaseKafkaEvent.<T>builder()
                .eventId(eventId)
                .type(type)
                .timestamp(now)
                .data(data)
                .build();

        String jsonPayload = serializePayload(fullEvent);

        OutboxEventEntity eventEntity = OutboxEventEntity.builder()
                .id(eventId)
                .eventType(type)
                .payload(jsonPayload)
                .status(OutboxStatus.PENDING)
                .createdAt(now)
                .retryCount(0)
                .build();

        outboxRepository.save(eventEntity);
    }


    private String serializePayload(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox payload for event", e);
        }
    }
}
