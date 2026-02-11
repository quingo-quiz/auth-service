package tech.arhr.quingo.auth_service.data.sql;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    List<UserEntity> findByEmailEqualsAndHashedPasswordEquals(String email, String hashedPassword);

    boolean existsByEmail(String email);
}
