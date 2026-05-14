package com.smartmailer.email;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmailQueueConfig {
    @Bean
    Queue emailQueue(@Value("${app.email.queue}") String queueName) {
        return new Queue(queueName, true);
    }
}
