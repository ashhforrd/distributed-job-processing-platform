package com.example.jobs.api.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByIdempotencyKey(String idempotencyKey);

    @Query(
        value = """
                SELECT *
                FROM jobs
                WHERE status = :status
                    AND scheduled_at <= :scheduledAt
                ORDER BY scheduled_at ASC
                LIMIT :batchSize
                FOR UPDATE SKIP LOCKED
                """,
            nativeQuery = true
    )
    List<JobEntity> findDueJobsForUpdate(
        @Param("status") String status,
        @Param("scheduledAt") Instant scheduledAt,
        @Param("batchSize") int batchSize
    );
}