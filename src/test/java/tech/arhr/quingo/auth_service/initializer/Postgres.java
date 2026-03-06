package tech.arhr.quingo.auth_service.initializer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

public final class Postgres {
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17");

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.datasource.username=" + postgres.getUsername()
            ).applyTo(applicationContext);
        }
    }
}

