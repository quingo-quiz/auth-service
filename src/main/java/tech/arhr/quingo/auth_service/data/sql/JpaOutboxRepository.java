package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.sql.entity.OutboxEventEntity;

import java.util.UUID;

@Repository
public interface JpaOutboxRepository extends JpaRepository<OutboxEventEntity, UUID> {
}
