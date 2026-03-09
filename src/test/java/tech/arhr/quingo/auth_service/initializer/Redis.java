package tech.arhr.quingo.auth_service.initializer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;

public final class Redis {
    public static final GenericContainer<?> redisContainer
            = new GenericContainer<>("redis:8.6-alpine")
            .withExposedPorts(6379)
            .withReuse(true);;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.data.redis.host=" + redisContainer.getHost(),
                    "spring.data.redis.port=" + redisContainer.getFirstMappedPort(),
                    "spring.data.redis.password="
            ).applyTo(applicationContext);
        }
    }
}
