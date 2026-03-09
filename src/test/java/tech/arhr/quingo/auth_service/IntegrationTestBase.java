package tech.arhr.quingo.auth_service;


import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import tech.arhr.quingo.auth_service.initializer.Postgres;
import tech.arhr.quingo.auth_service.initializer.Redis;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers = {
        Postgres.Initializer.class,
        Redis.Initializer.class
})
public abstract class IntegrationTestBase {

    @LocalServerPort
    protected int port;

    @BeforeAll
    static void beforeAll() {
        Postgres.postgresContainer.start();
        Redis.redisContainer.start();
    }
}
