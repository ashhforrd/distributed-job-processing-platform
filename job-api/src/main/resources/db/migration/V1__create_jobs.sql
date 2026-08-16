CREATE TABLE jobs (
    id UUID PRIMARY KEY,
    type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    last_error TEXT,
    idempotency_key VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT jobs_status_check CHECK (
        status IN (
            'PENDING',
            'QUEUED',
            'RUNNING',
            'SUCCEEDED',
            'FAILED',
            'DEAD_LETTERED',
            'CANCELLED'
        )
    ),
    CONSTRAINT jobs_attempts_check CHECK (attempts >= 0),
    CONSTRAINT jobs_max_attempts_check CHECK (max_attempts >= 1),
    CONSTRAINT jobs_attempt_limit_check CHECK (attempts <= max_attempts),
    CONSTRAINT jobs_idempotency_key_unique UNIQUE (idempotency_key)
);

CREATE INDEX jobs_status_scheduled_at_idx
    ON jobs (status, scheduled_at);

CREATE INDEX jobs_created_at_idx
    ON jobs (created_at DESC);