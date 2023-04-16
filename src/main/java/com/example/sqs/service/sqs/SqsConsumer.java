package com.example.sqs.service.sqs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface SqsConsumer {
    String value();
    int concurrent() default 1;
    String concurrentString() default "";
    int waitSeconds() default 10;
    String waitSecondsString() default "";
}
