package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.sql.entity.OutboxEventEntity;
import tech.arhr.quingo.auth_service.enums.OutboxStatus;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
    List<OutboxEventEntity> findTopByStatusOrderByCreatedAtAsc(OutboxStatus status, Limit limit);
}
