package tech.arhr.quingo.auth_service.utils;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

@Component
public class TimeProvider {
    private volatile Clock clock = Clock.systemUTC();

    public Instant now() {
        return Instant.now(clock);
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
