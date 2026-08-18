package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;
import com.example.jobs.domain.JobStatus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobProcessor {

    private static final Logger log =
        LoggerFactory.getLogger(JobProcessor.class);
    
    private final JobStateService jobStateService;
    private final JobDispatcher jobDispatcher;

    public JobProcessor(
        JobStateService jobStateService,
        JobDispatcher jobDispatcher
    ) {
        this.jobStateService = jobStateService;
        this.jobDispatcher = jobDispatcher;
    }

    public void process(JobMessage message) {
        boolean shouldProcess = 
            jobStateService.markRunning(message.jobId());

        if (!shouldProcess) {
            log.info(
                "Skipping terminal job: id={}",
                message.jobId()
            );
            return;
        }

        try {
            jobDispatcher.dispatch(message);
            jobStateService.markSucceeded(message.jobId());

            log.info(
                "Job completed successfully: id={}",
                message.jobId()
            );
        } catch(RuntimeException exception) {
            String errorMessage = exception.getMessage() != null 
                ? exception.getMessage() 
                : exception.getClass().getSimpleName();
            
            JobStatus status = jobStateService.markFailed(message.jobId(), errorMessage);

            log.error(
                "Job processing failed: id={}, status={}, error={}",
                message.jobId(),
                status,
                errorMessage
            );

            throw exception;
        }
    }

}