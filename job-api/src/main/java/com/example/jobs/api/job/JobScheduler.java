package com.example.jobs.api.job;

import com.example.jobs.domain.JobStatus;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Component
public class JobScheduler {

    private static final int BATCH_SIZE = 100;

    private final JobRepository jobRepository;
    private final ApplicationEventPublisher eventPublisher;

    public JobScheduler(
        JobRepository jobRepository,
        ApplicationEventPublisher eventPublisher
    ) {
        this.jobRepository = jobRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(fixedDelayString = "${app.scheduling.poll-interval-ms:1000}")
    @Transactional
    public void enqueueDueJobs() {
        List<JobEntity> dueJobs =
            jobRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                JobStatus.PENDING, 
                Instant.now(), 
                PageRequest.of(0, BATCH_SIZE));
        
        for (JobEntity job : dueJobs) {
            job.markQueued();
            eventPublisher.publishEvent(new JobCreatedEvent(job));
        }
    }
}