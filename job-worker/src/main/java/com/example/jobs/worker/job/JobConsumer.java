package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class JobConsumer {

    private static final Logger log = 
        LoggerFactory.getLogger(JobConsumer.class);
    
    private final JobProcessor jobProcessor;

    public JobConsumer(JobProcessor jobProcessor) {
        this.jobProcessor = jobProcessor;
    }
    
    @KafkaListener(topics = "${app.kafka.job-topic}")
    public void consume(JobMessage message) {
        log.info(
            "Received job: id={}, type={}, attempt={}",
            message.jobId(),
            message.type(),
            message.attempt()
        );

        jobProcessor.process(message);
    }
}