package com.example.sqs.service;

import com.example.sqs.service.dto.Request;
import com.example.sqs.service.sqs.SqsBinder;
import com.example.sqs.service.sqs.SqsConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@SqsBinder
public class ScrapService {
    @SqsConsumer(value = "${app.sqs.queue.test.name}",
            concurrentString = "${app.sqs.queue.test.concurrent:1}",
            waitSecondsString = "${app.sqs.queue.test.waitSeconds:10}")
    public void requestScrap(Request request) {
        log.info("scrap {}", request);
    }
}
