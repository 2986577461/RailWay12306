package com.xiaoyan.railway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Two breakers so the blast radius stays separate: inventory failures must not block search,
 * and a failing search backend must not stop orders from being accepted.
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreaker redisInventoryBreaker() {
        return CircuitBreaker.of("redis-inventory", breakerConfig());
    }

    @Bean
    public CircuitBreaker redisQueryBreaker() {
        return CircuitBreaker.of("redis-query", breakerConfig());
    }

    private static CircuitBreakerConfig breakerConfig() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(20)
                .minimumNumberOfCalls(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }
}
