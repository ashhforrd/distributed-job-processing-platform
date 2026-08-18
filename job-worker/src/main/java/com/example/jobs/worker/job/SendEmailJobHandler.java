package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SendEmailJobHandler implements JobHandler {

    private static final Logger log = 
        LoggerFactory.getLogger(SendEmailJobHandler.class);

        @Override
        public String supportedType() {
            return "SEND_EMAIL";
        }

        @Override
        public void handle(JobMessage message) {
            Object recipient = message.payload().get("to");
            Object subject = message.payload().get("subject");

            if (recipient == null) {
                throw new IllegalArgumentException(
                    "SEND_EMAIL payload requires field 'to'"
                );
            }

            log.info("Simulating email delivery: jobId={}, to={}, subject={}",
                message.jobId(),
                recipient,
                subject
            );
        
        }
}