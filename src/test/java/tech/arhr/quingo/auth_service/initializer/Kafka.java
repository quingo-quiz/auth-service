package tech.arhr.quingo.auth_service.initializer;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.kafka.KafkaContainer;

public final class Kafka {
    public static final KafkaContainer kafkaContainer =
            new KafkaContainer("apache/kafka:4.0.2")
                    .withReuse(true);

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            TestPropertyValues.of(
                    "spring.kafka.bootstrap-servers=" + kafkaContainer.getBootstrapServers()
            ).applyTo(applicationContext);
        }
    }
}
