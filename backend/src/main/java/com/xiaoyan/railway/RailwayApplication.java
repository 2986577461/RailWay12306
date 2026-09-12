package com.xiaoyan.railway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Railway platform monolith application.
 * Replaces the former Spring Cloud microservices (gateway + 8 services).
 * All REST controllers and RocketMQ listeners live in this single JVM;
 * external dependencies are only MySQL, Redis and RocketMQ.
 */
@EnableScheduling
@SpringBootApplication
public class RailwayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RailwayApplication.class, args);
    }
}
