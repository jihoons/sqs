package com.example.sqs.service.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class SQSListenerFactory {
    private final ApplicationContext applicationContext;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final Map<String, SqsConsumerInfo> map = new HashMap<>();
    private List<SQSListener> listeners;
    private ExecutorService executor;
    private final PropertyResolver propertyResolver;

    @PostConstruct
    public void start() {
        int counter = 0;
        Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(SqsBinder.class);
        for (Map.Entry<String, Object> entry : beansWithAnnotation.entrySet()) {
            Object bean = entry.getValue();
            counter += findSqsListenerMethods(bean);
        }
        this.listeners = new ArrayList<>();

        if (counter > 0)
            executor = Executors.newFixedThreadPool(counter);
        for (Map.Entry<String, SqsConsumerInfo> entry : map.entrySet()) {
            String queueName = entry.getKey();
            SqsConsumerInfo consumer = entry.getValue();
            SQSListener sqsListener = new SQSListener(sqsClient, consumer, objectMapper, executor);
            this.listeners.add(sqsListener);
            sqsListener.start("sqsListener-" + queueName);
        }
    }

    private int findSqsListenerMethods(Object bean) {
        int counter = 0;
        AopUtils.getTargetClass(bean);
        for (Method method : AopUtils.getTargetClass(bean).getDeclaredMethods()) {
            SqsConsumer sqsConsumer = AnnotationUtils.findAnnotation(method, SqsConsumer.class);
            if (sqsConsumer == null) {
                continue;
            }

            String queueName = sqsConsumer.value();
            if (queueName != null && !queueName.isBlank()) {
                counter += addMethod(sqsConsumer, bean, method);
            }
        }
        return counter;
    }

    private int addMethod(SqsConsumer sqsConsumer, Object bean, Method method) {
        String queueName = propertyResolver.resolvePlaceholders(sqsConsumer.value());
        SqsConsumerInfo consumer = map.get(queueName);
        if (consumer != null) {
            log.error("{} is duplicated. {} and {}", queueName, consumer.getMethod(), method);
            throw new RuntimeException(queueName + " queue duplicated");
        }

        String queueUrl = getQueueUrl(queueName);
        Parameter[] parameters = method.getParameters();
        if (parameters != null && parameters.length > 1) {
            throw new RuntimeException("sqs listener method can have only 1 argument.[" + method.getDeclaringClass().getName() + "." + method.getName() + "]");
        }

        String concurrentStr = sqsConsumer.concurrentString();
        int concurrentCount = sqsConsumer.concurrent();
        if (concurrentStr != null && !concurrentStr.isBlank()) {
            String value = propertyResolver.resolveRequiredPlaceholders(concurrentStr);
            concurrentCount = Integer.parseInt(value);
        }

        String waitSecondsStr = sqsConsumer.waitSecondsString();
        int waitSeconds = sqsConsumer.waitSeconds();
        if (waitSecondsStr != null && !waitSecondsStr.isBlank()) {
            String value = propertyResolver.resolveRequiredPlaceholders(waitSecondsStr);
            waitSeconds = Integer.parseInt(value);
        }

        map.put(queueName, SqsConsumerInfo.builder()
                .queueUrl(queueUrl)
                .queueName(queueName)
                .bean(bean)
                .method(method)
                .concurrent(concurrentCount)
                .waitSeconds(waitSeconds)
                .parameters(parameters)
                .build());
        return concurrentCount > 1 ? concurrentCount : 0;
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

    @PreDestroy
    public void end() {
        log.info("end");
        this.listeners.forEach(SQSListener::stop);
        Optional<SQSListener> option = this.listeners.stream().filter(SQSListener::isProcessing).findFirst();
        while (option.isPresent()) {
            try {
                TimeUnit.MILLISECONDS.sleep(50);
            } catch (InterruptedException ignore) {
            }
        }
    }
}
