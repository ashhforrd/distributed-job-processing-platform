package com.example.jobs.api.job;

import com.example.jobs.domain.JobStatus;
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
        if (event.job().getStatus() == JobStatus.QUEUED) {
            jobPublisher.publish(event.job());
        }
    }
}