package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import org.springframework.stereotype.Component;
import tech.arhr.quingo.auth_service.data.entity.UserEntity;
import tech.arhr.quingo.auth_service.dto.UserDto;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserEntity entity);

    @Mapping(target = "hashedPassword", ignore = true)
    UserEntity toEntity(UserDto dto);
}
