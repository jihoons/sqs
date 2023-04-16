package com.example.sqs.service.sqs;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Data
@Builder
public class SqsConsumerInfo {
    private Object bean;
    private Method method;
    private String queueName;
    private String queueUrl;
    @Builder.Default
    private int concurrent = 1;
    @Builder.Default
    private int waitSeconds = 10;
    private Parameter[] parameters;
}
