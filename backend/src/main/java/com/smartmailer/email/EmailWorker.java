package com.smartmailer.email;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailWorker {
    private final EmailService emailService;

    public EmailWorker(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = "${app.email.queue}")
    public void handle(EmailJob job) {
        emailService.process(job);
    }
}
