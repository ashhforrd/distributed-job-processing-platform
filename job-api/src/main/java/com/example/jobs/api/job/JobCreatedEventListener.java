package com.example.jobs.api.job;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class JobCreatedEventListener {

    private final JobPublisher jobPublisher;

    public JobCreatedEventListener(JobPublisher jobPublisher) {
        this.jobPublisher = jobPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJobCreated(JobCreatedEvent event) {
        jobPublisher.publish(event.job());
    }
}