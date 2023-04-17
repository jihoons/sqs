package com.example.sqs.service.sqs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RequiredArgsConstructor
@Component
public class SqsPublisher {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> queueNameMap = new ConcurrentHashMap<>();
    public <T extends ContentBaseDuplicatedMessage> boolean publishForContentBaseDuplicatedMessage(String queueName, String groupId, T body) {
        String queueUrl = queueNameMap.computeIfAbsent(queueName, this::getQueueUrl);
        SendMessageRequest request = SendMessageRequest
                .builder()
                .queueUrl(queueUrl)
                .messageGroupId(groupId)
                .messageDeduplicationId(body.getDuplicatedContent())
                .messageBody(getBody(body))
                .build();
        SendMessageResponse response = sqsClient.sendMessage(request);
        return response.sdkHttpResponse().isSuccessful();
    }

    private <T> String getBody(T body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String getQueueUrl(String queueName) {
        GetQueueUrlRequest request = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        GetQueueUrlResponse response = null;
        try {
            response = sqsClient.getQueueUrl(request);
        } catch (Exception e) {
            log.error("get {} queue info fail", queueName, e);
            throw new RuntimeException(e);
        }
        log.debug("queue : {}, url : {}", queueName, response.queueUrl());

        return response.queueUrl();
    }
}
