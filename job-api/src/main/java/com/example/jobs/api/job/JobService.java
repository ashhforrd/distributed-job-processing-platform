package com.example.jobs.api.job;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JobService(JobRepository jobRepository, ApplicationEventPublisher eventPublisher) {
        this.jobRepository = jobRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public JobResponse create(CreateJobRequest request) {
        String idempotencyKey = normalize(request.idempotencyKey());

        Optional<JobEntity> existingJob = findByIdempotencyKey(idempotencyKey);

        if (existingJob.isPresent()) {
            return JobResponse.from(existingJob.get());
        }

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

    @Transactional(readOnly = true)
    public JobResponse findById(UUID jobId) {
        return jobRepository.findById(jobId)
            .map(JobResponse::from)
            .orElseThrow(() -> new JobNotFoundException(jobId));
    }


    private Optional<JobEntity> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            return Optional.empty();
        }

        return jobRepository.findByIdempotencyKey(idempotencyKey);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}