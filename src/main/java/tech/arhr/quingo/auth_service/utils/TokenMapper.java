package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.arhr.quingo.auth_service.api.rest.models.TokenModel;
import tech.arhr.quingo.auth_service.data.redis.models.TokenRedisModel;
import tech.arhr.quingo.auth_service.data.sql.entity.TokenEntity;
import tech.arhr.quingo.auth_service.dto.TokenDto;

@Mapper(componentModel = "spring")
public interface TokenMapper {
    @Mapping(source = "userDto", target = "user")
    TokenEntity toEntity(TokenDto dto);

    @Mapping(source = "user", target = "userDto")
    TokenDto toDto(TokenEntity entity);

    @Mapping(source = "id", target = "tokenId")
    @Mapping(source = "userDto.id", target = "userId")
    @Mapping(source = "expiresAt", target = "expireTime")
    TokenRedisModel toRedisModel(TokenDto dto);

    @Mapping(source = "id", target = "tokenId")
    TokenModel toApiModel(TokenDto dto);
}
