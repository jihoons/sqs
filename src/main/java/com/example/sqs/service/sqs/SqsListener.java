package com.example.sqs.service.sqs;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
public class SqsListener implements Runnable {
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger processingCounter = new AtomicInteger(0);

    private final SqsClient sqsClient;
    private final SqsConsumerInfo consumer;
    private final ObjectMapper objectMapper;
    private final ExecutorService executorService;

    public void start(String threadName) {
        running.set(true);
        Thread thread = new Thread(this, threadName);
        thread.start();
    }

    public void stop() {
        running.set(false);
    }

    public boolean isProcessing() {
        return processingCounter.get() > 0;
    }

    @Override
    public void run() {
        while (running.get()) {
            if (!canExecute()) {
                log.debug("wait worker {}", processingCounter.get());
                waitWorker();
                continue;
            }

            List<Message> messages = getMessage(consumer.getConcurrent() - processingCounter.get());
            if (messages == null) {
                log.debug("empty messages");
                continue;
            }

            for (Message message : messages) {
                if (!running.get())
                    break;
                processingCounter.incrementAndGet();

                deleteMessage(message);
                log.debug(message.body());
                try {
                    processMessage(message);
                } catch (Exception e) {
                    log.error("error", e);
                }
            }
        }
    }

    private void processMessage(Message message) {
        if (consumer.getConcurrent() > 1) {
            executorService.submit(() -> {
                callConsumer(message.body(), consumer);
            });
        } else {
            callConsumer(message.body(), consumer);
        }
    }

    private boolean canExecute() {
        log.debug("processingCounter : {}", processingCounter.get());
        if (processingCounter.get() >= consumer.getConcurrent()) {
            return false;
        }
        return true;
    }

    private void waitWorker() {
        try {
            TimeUnit.MILLISECONDS.sleep(100);
        } catch (InterruptedException ignore) {
        }
    }

    private void callConsumer(String body, SqsConsumerInfo consumer) {
        try {
            Object[] arguments = getArguments(body, consumer.getParameters());
            if (consumer.getParameters() == null || consumer.getParameters().length == 0)
                consumer.getMethod().invoke(consumer.getBean());
            else
                consumer.getMethod().invoke(consumer.getBean(), arguments);
        } catch (Exception e) {
            log.error("consumer invoke error", e);
        }
        processingCounter.decrementAndGet();
        log.debug("decrement processingCounter : {}", processingCounter.get());
    }

    private Object[] getArguments(String body, Parameter[] parameters) {
        if (parameters == null || parameters.length == 0)
            return null;

        return Arrays.stream(parameters).map(parameter -> {
            if (parameter.getType().equals(String.class)) {
                return body;
            } else {
                try {
                    return objectMapper.readValue(body, parameter.getType());
                } catch (Exception e) {
                    log.error("parsing error {}", body, e);
                    throw new RuntimeException(e);
                }
            }
        }).toList().toArray();
    }

    private List<Message> getMessage(int maxMessageCount) {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
                .queueUrl(this.consumer.getQueueUrl())
                .maxNumberOfMessages(maxMessageCount)
                .waitTimeSeconds(this.consumer.getWaitSeconds())
                .build();
        ReceiveMessageResponse response = sqsClient.receiveMessage(request);
        if (!response.hasMessages() || CollectionUtils.isEmpty(response.messages()))
            return null;
        return response.messages();
    }

    private void deleteMessage(Message message) {
        DeleteMessageRequest request = DeleteMessageRequest.builder()
                .queueUrl(this.consumer.getQueueUrl())
                .receiptHandle(message.receiptHandle())
                .build();
        try {
            sqsClient.deleteMessage(request);
        } catch (Exception e) {
            log.error("delete fail {}", message.receiptHandle(), e);
        }
    }
}
