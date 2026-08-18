package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;
import com.example.jobs.domain.JobStatus;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class JobProcessor {

    private static final Logger log =
        LoggerFactory.getLogger(JobProcessor.class);

    private final JobStateService jobStateService;
    private final JobDispatcher jobDispatcher;
    private final MeterRegistry meterRegistry;

    public JobProcessor(
        JobStateService jobStateService,
        JobDispatcher jobDispatcher,
        MeterRegistry meterRegistry
    ) {
        this.jobStateService = jobStateService;
        this.jobDispatcher = jobDispatcher;
        this.meterRegistry = meterRegistry;
    }

    public void process(JobMessage message) {
        boolean shouldProcess =
            jobStateService.markRunning(message.jobId());

        if (!shouldProcess) {
            incrementMetric("skipped");

            log.info(
                "Skipping terminal job: id={}",
                message.jobId()
            );
            return;
        }

        try {
            jobDispatcher.dispatch(message);
            jobStateService.markSucceeded(message.jobId());
            incrementMetric("succeeded");

            log.info(
                "Job completed successfully: id={}",
                message.jobId()
            );
        } catch(RuntimeException exception) {
            String errorMessage = exception.getMessage() != null
                ? exception.getMessage()
                : exception.getClass().getSimpleName();

            JobStatus status = jobStateService.markFailed(
                message.jobId(),
                errorMessage
            );

            String outcome = status == JobStatus.DEAD_LETTERED ? "dead_lettered" : "retryable_failure";

            incrementMetric(outcome);

            log.error(
                "Job processing failed: id={}, status={}, error={}",
                message.jobId(),
                status,
                errorMessage
            );

            throw exception;
        }
    }

    private void incrementMetric(String outcome) {
        meterRegistry.counter(
            "jobs.processed",
            "outcome",
            outcome
        ).increment();
    }


}
