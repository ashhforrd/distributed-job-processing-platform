package com.example.jobs.api.job;

import com.example.jobs.domain.JobMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
public class JobPublisher {

    private final KafkaTemplate<String, JobMessage> kafkaTemplate;
    private final String topicName;

    public JobPublisher(
        KafkaTemplate<String, JobMessage> kafkaTemplate,
        @Value("${app.kafka.job-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public CompletableFuture<SendResult<String, JobMessage>> publish(
        JobEntity job
    ) {
        JobMessage message = new JobMessage(
            job.getId(),
            job.getType(),
            job.getPayload(),
            job.getAttempts(),
            job.getCreatedAt()
        );

        return kafkaTemplate.send(
            topicName,
            job.getId().toString(),
            message
        );
    }
}