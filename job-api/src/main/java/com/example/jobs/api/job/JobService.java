package com.example.jobs.api.job;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final JobCreator jobCreator;

    public JobService(
        JobRepository jobRepository,
        JobCreator jobCreator
    ) {
        this.jobRepository = jobRepository;
        this.jobCreator = jobCreator;
    }

    public JobResponse create(CreateJobRequest request) {
        String idempotencyKey = normalize(request.idempotencyKey());

        Optional<JobEntity> existingJob =
            findByIdempotencyKey(idempotencyKey);

        if (existingJob.isPresent()) {
            return JobResponse.from(existingJob.get());
        }

        try {
            return jobCreator.createNew(request, idempotencyKey);
        } catch (DataIntegrityViolationException exception) {
            if (idempotencyKey == null) {
                throw exception;
            }

            return jobRepository.findByIdempotencyKey(idempotencyKey)
                .map(JobResponse::from)
                .orElseThrow(() -> exception);
        }
    }

    @Transactional(readOnly = true)
    public JobResponse findById(UUID jobId) {
        return jobRepository.findById(jobId)
            .map(JobResponse::from)
            .orElseThrow(() -> new JobNotFoundException(jobId));
    }

    private Optional<JobEntity> findByIdempotencyKey(
        String idempotencyKey
    ) {
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