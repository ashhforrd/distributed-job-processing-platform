package com.example.jobs.worker.job;

import com.example.jobs.domain.JobMessage;

public interface JobHandler {
    
    String supportedType();

    void handle(JobMessage message);
}
