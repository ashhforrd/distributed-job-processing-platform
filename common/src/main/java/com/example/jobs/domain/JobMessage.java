package com.example.jobs.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JobMessage(
    UUID jobId,
    String type,
    Map<String, Object> payload,
    int attempt,
    Instant createdAt
) {

}