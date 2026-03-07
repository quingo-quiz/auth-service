package tech.arhr.quingo.auth_service.data.redis;

import tech.arhr.quingo.auth_service.dto.TokenDto;

public interface RedisTokenRepository {
    void save(String jti);
}
