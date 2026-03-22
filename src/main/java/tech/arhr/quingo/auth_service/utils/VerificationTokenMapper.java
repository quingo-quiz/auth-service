package tech.arhr.quingo.auth_service.utils;

import org.mapstruct.Mapper;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.dto.VerificationTokenDto;

@Mapper(componentModel = "spring")
public interface VerificationTokenMapper {
    VerificationTokenDto toDto(VerificationTokenRedisModel model);

    VerificationTokenRedisModel toModel(VerificationTokenDto dto);
}
