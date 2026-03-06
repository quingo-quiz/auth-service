package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.dto.TokenDto;

@Mapper(componentModel = "spring")
public interface TokenMapper {
    @Mapping(source = "userDto", target = "user")
    TokenEntity toEntity(TokenDto dto);

    @Mapping(source = "user", target = "userDto")
    TokenDto toDto(TokenEntity entity);
}
