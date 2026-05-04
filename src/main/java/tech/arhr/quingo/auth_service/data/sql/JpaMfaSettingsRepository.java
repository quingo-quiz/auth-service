package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.arhr.quingo.auth_service.data.sql.entity.UserMfaSettingsEntity;
import tech.arhr.quingo.auth_service.enums.MfaType;

import java.util.List;
import java.util.UUID;

public interface JpaMfaSettingsRepository extends JpaRepository<UserMfaSettingsEntity, UUID> {
    List<UserMfaSettingsEntity> findByUserIdAndType(UUID userId, MfaType type);

    void deleteByUserIdEqualsAndType(UUID userId, MfaType type);

    List<UserMfaSettingsEntity> findByUserId(UUID userId);
}
