package com.example.jobs.api.job;

import com.example.jobs.domain.JobStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);

    List<JobEntity> findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
        JobStatus status,
        Instant scheduledAt,
        Pageable pageable
    );
}