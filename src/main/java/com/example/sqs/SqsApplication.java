package com.example.sqs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Map;

@SpringBootApplication
public class SqsApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(SqsApplication.class);
        application.setDefaultProperties(Map.of("spring.profiles.default", "dev"));
        application.run(args);
    }
}
