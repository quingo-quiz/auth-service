package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import tech.arhr.quingo.auth_service.data.sql.entity.SocialAccountEntity;
import tech.arhr.quingo.auth_service.data.sql.entity.UserEntity;
import tech.arhr.quingo.auth_service.dto.SocialAccountDto;
import tech.arhr.quingo.auth_service.dto.UserDto;

@Mapper(componentModel = "spring")
public interface SocialAccountMapper {
    SocialAccountDto toDto(SocialAccountEntity entity);
}
