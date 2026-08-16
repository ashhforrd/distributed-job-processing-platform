package com.example.jobs.domain;

public enum JobStatus {
    PENDING,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    DEAD_LETTERED,
    CANCELLED
}