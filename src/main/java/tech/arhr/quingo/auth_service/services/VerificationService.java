package tech.arhr.quingo.auth_service.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tech.arhr.quingo.auth_service.data.redis.interfaces.RedisVerificationTokenRepository;
import tech.arhr.quingo.auth_service.data.redis.models.VerificationTokenRedisModel;
import tech.arhr.quingo.auth_service.dto.UserDto;
import tech.arhr.quingo.auth_service.dto.VerificationTokenDto;
import tech.arhr.quingo.auth_service.utils.VerificationTokenMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VerificationService {
    private final RedisVerificationTokenRepository redisRepository;
    private final VerificationTokenMapper mapper;


    private VerificationTokenDto generateToken(UserDto userDto) {
        var token = new VerificationTokenDto();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(userDto.getId());

        VerificationTokenRedisModel model = mapper.toModel(token);
        redisRepository.save(model);
        return token;
    }

    public void sendVerificationToken(UserDto userDto) {
        generateToken(userDto);
    }

    public boolean validateToken(String token){
        return redisRepository.exists(token);
    }

}
