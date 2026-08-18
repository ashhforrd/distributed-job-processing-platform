package com.example.jobs.worker.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WorkerJobRepository extends JpaRepository<WorkerJobEntity, UUID> {
    
}