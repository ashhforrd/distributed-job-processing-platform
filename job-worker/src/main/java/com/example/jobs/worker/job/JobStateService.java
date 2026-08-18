package com.example.jobs.worker.job;

import com.example.jobs.domain.JobStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class JobStateService {

    private final WorkerJobRepository jobRepository;

    public JobStateService(WorkerJobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markRunning(UUID jobId) {
        WorkerJobEntity job = findJob(jobId);

        if (job.getStatus() == JobStatus.SUCCEEDED || job.getStatus() == JobStatus.DEAD_LETTERED) {
            return false;
        }

        job.markRunning();
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSucceeded(UUID jobId) {
        WorkerJobEntity job = findJob(jobId);
            job.markSucceeded();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public JobStatus markFailed(UUID jobId, String errorMessage) {
        WorkerJobEntity job = findJob(jobId);
        job.markFailed(errorMessage);
        return job.getStatus();
    }

    private WorkerJobEntity findJob(UUID jobId) {
        return jobRepository.findById(jobId)
            .orElseThrow(() -> new IllegalArgumentException(
                "Job not found: " + jobId
            ));
    }
}