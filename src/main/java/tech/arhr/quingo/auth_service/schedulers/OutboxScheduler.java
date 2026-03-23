package tech.arhr.quingo.auth_service.schedulers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tech.arhr.quingo.auth_service.data.sql.JpaOutboxRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.OutboxEventEntity;
import tech.arhr.quingo.auth_service.enums.OutboxStatus;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final JpaOutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String NOTIFICATIONS_TOPIC = "notifications-events";

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutboxMessages() {
        List<OutboxEventEntity> events = outboxRepository.findTopByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING, Limit.of(20));

        if (events.isEmpty()) return;

        for (OutboxEventEntity event : events) {
            try {
                kafkaTemplate.send(NOTIFICATIONS_TOPIC, event.getId().toString(), event.getPayload())
                        .get();

                event.setStatus(OutboxStatus.SENT);
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                if (event.getRetryCount() >= 5) {
                    event.setStatus(OutboxStatus.FAILED);
                }
            }
        }

        outboxRepository.saveAll(events);
    }
}
