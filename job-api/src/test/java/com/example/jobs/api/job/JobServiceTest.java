package com.example.jobs.api.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private JobCreator jobCreator;

    @Test
    void returnsExistingJobWhenConcurrentInsertUsesSameIdempotencyKey() {
        String idempotencyKey = "concurrent-request-001";

        CreateJobRequest request = new CreateJobRequest(
            "SEND_EMAIL",
            Map.of("to", "student@example.com"),
            3,
            Instant.now(),
            idempotencyKey
        );

        JobEntity existingJob = new JobEntity(
            request.type(),
            request.payload(),
            request.maxAttempts(),
            request.scheduledAt(),
            idempotencyKey
        );

        when(jobRepository.findByIdempotencyKey(idempotencyKey))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingJob));

        when(jobCreator.createNew(request, idempotencyKey))
            .thenThrow(
                new DataIntegrityViolationException("duplicate key")
            );

        JobService jobService =
            new JobService(jobRepository, jobCreator);

        JobResponse response = jobService.create(request);

        assertEquals(existingJob.getId(), response.id());

        verify(jobRepository, times(2))
            .findByIdempotencyKey(idempotencyKey);
    }
}