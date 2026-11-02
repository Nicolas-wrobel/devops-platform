package com.devops_platform.backend.common;

import java.time.Clock;
import java.util.Random;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Centralizes "now" and "randomness" behind beans so time- or chance-driven
// logic (see deployment/) can be unit-tested deterministically by swapping
// in a fixed Clock / seeded Random, instead of calling Instant.now()/new Random() directly.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public Random random() {
        return new Random();
    }
}
