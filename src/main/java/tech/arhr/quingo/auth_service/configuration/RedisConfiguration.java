    package tech.arhr.quingo.auth_service.configuration;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
    import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
    import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
    import org.springframework.data.redis.core.RedisTemplate;
    import org.springframework.data.redis.serializer.GenericToStringSerializer;
    import org.springframework.data.redis.serializer.StringRedisSerializer;

    @Configuration
    public class RedisConfiguration {
        @Value("${spring.data.redis.host}")
        private String  HOST;

        @Value("${spring.data.redis.port}")
        private int PORT;

        @Value("${spring.data.redis.password}")
        private String PASSWORD;

        @Bean
        public LettuceConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(HOST);
            config.setPort(PORT);
            config.setPassword(PASSWORD);

            return new LettuceConnectionFactory(config);
        }

        @Bean
        public RedisTemplate<String, Object> redisTemplate() {
            final RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();

            redisTemplate.setConnectionFactory(redisConnectionFactory());
            redisTemplate.setKeySerializer(new StringRedisSerializer());
            redisTemplate.setValueSerializer(new GenericToStringSerializer<Object>(Object.class));

            redisTemplate.afterPropertiesSet();
            return redisTemplate;
        }
    }
