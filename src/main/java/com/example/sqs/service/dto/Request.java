package com.example.sqs.service.dto;

import com.example.sqs.service.sqs.ContentBaseDuplicatedMessage;
import lombok.Data;

@Data
public class Request implements ContentBaseDuplicatedMessage {
    private String transactionNo;

    @Override
    public String getDuplicatedContent() {
        return transactionNo;
    }
}
