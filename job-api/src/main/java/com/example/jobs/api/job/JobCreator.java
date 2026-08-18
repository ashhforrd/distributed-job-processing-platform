package com.example.jobs.api.job;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class JobCreator {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JobCreator(
        JobRepository jobRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.jobRepository = jobRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobResponse createNew(
        CreateJobRequest request,
        String idempotencyKey
    ) {
        int maxAttempts = request.maxAttempts() == null
            ? DEFAULT_MAX_ATTEMPTS
            : request.maxAttempts();

        Instant now = Instant.now();
        Instant scheduledAt = request.scheduledAt() == null
            ? now
            : request.scheduledAt();

        JobEntity job = new JobEntity(
            request.type().trim(),
            request.payload(),
            maxAttempts,
            scheduledAt,
            idempotencyKey
        );

        if (!scheduledAt.isAfter(now)) {
            job.markQueued();
        }

        JobEntity savedJob = jobRepository.saveAndFlush(job);
        eventPublisher.publishEvent(new JobCreatedEvent(savedJob));

        return JobResponse.from(savedJob);
    }
}
