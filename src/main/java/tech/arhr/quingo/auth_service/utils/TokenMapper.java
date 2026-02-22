package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import tech.arhr.quingo.auth_service.data.entity.TokenEntity;
import tech.arhr.quingo.auth_service.dto.TokenDto;

@Mapper
public interface TokenMapper {
    TokenMapper INSTANCE = Mappers.getMapper(TokenMapper.class);

    TokenEntity toEntity(TokenDto dto);

    TokenDto toDto(TokenEntity entity);
}
