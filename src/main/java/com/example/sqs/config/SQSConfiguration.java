package com.example.sqs.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
@Configuration
public class SQSConfiguration {
    @Bean
    public SqsClient sqsClient() {
        return SqsClient.builder()
                .region(Region.AP_NORTHEAST_2)
                .build();
    }

//    @Value("${app.sqs.queue}")
//    private String queueName;
//
//    @Bean
//    public SQSListener sqsListener(ApplicationContext applicationContext, ObjectMapper objectMapper, SqsClient sqsClient) {
//        return new SQSListener(applicationContext, objectMapper, sqsClient, queueName);
//    }
}
