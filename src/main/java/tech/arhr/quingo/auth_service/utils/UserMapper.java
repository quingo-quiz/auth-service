package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.arhr.quingo.auth_service.api.rest.models.UserApiModel;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.UserProfileDto;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDto toDto(UserEntity entity);


    UserEntity toEntity(UserDto dto);

    UserApiModel toApiModel(UserDto dto);

    UserProfileDto toProfileDto(UserEntity entity);
}
