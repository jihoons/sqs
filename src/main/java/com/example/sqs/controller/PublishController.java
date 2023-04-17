package com.example.sqs.controller;

import com.example.sqs.service.dto.Request;
import com.example.sqs.service.sqs.SqsPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
public class PublishController {
    private final SqsPublisher publisher;
    @PostMapping("/sqs/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public void publish(@RequestBody Request request) {
        publisher.publishForContentBaseDuplicatedMessage("scrap.fifo", "scraper", request);
    }
}
