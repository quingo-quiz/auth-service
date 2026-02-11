package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTokenRepository extends JpaRepository<TokenEntity, UUID> {
    List<TokenEntity> findAllByUserId(UUID userId);
}
