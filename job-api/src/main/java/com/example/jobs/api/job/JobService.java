package com.example.jobs.api.job;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private static final int DEFAULT_MAX_ATTEMPTS = 3;

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
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

        Instant scheduledAt = request.scheduledAt() == null 
            ? Instant.now()
            : request.scheduledAt();
    
        JobEntity job = new JobEntity(
            request.type().trim(),
            request.payload(),
            maxAttempts,
            scheduledAt,
            idempotencyKey
        );

        JobEntity savedJob = jobRepository.saveAndFlush(job);

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