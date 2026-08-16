package com.example.jobs.api.job;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Map;

public record CreateJobRequest(
    @NotBlank
    @Size(max = 100)
    String type,

    @NotNull
    Map<String, Object> payload,

    @Min(1)
    @Max(100)
    Integer maxAttempts,

    Instant scheduledAt,

    @Size(max = 255)
    String idempotencyKey
) {
   
}
