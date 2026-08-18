# Distributed Job Processing Platform

## Overview

A distributed backend for accepting, scheduling, and asynchronously processing jobs. The project separates HTTP job submission from background execution and uses PostgreSQL as the source of truth, Kafka as the work queue, and Redis as available shared infrastructure.

The Maven multi-module project contains:

- `common`: shared job messages and status types
- `job-api`: REST API, persistence, scheduling, and Kafka publishing
- `job-worker`: Kafka consumption, handler dispatch, retries, dead-letter handling, and metrics

The stack uses Java 21, Spring Boot 3, Maven, PostgreSQL, Kafka, Redis, and Docker Compose.

## Problem

Executing slow or failure-prone work inside an HTTP request couples client latency to processing time and makes retries difficult. A job platform should accept work quickly, persist its state, process it independently, recover from transient failures, and expose enough state to diagnose failures.

This project addresses those concerns by separating job submission from job execution and communicating asynchronously through Kafka.

## Requirements

The platform must:

- accept and query jobs through an HTTP API
- persist job payloads and lifecycle state
- process jobs asynchronously
- support immediate and scheduled execution
- prevent duplicate submissions with idempotency keys
- retry transient failures
- route exhausted jobs to a dead-letter topic
- allow multiple workers to process jobs in parallel
- expose health checks and metrics
- run locally through Docker Compose

## Architecture

```text
                         +----------------+
Client ---------------->|    Job API     |
                         +-------+--------+
                                 |
                    persist      | publish
                                 |
                         +-------v--------+
                         |   PostgreSQL   |
                         +----------------+
                                 ^
                                 |
                         +-------+--------+
                         | Kafka jobs.v1  |
                         +--+----------+--+
                            |          |
                      +-----v--+   +---v-----+
                      | Worker |   | Worker  |
                      +--------+   +---------+
                            |
                     exhausted failures
                            |
                     +------v-------+
                     | jobs.v1.DLT  |
                     +--------------+
```

The API and workers are independently deployable services. Kafka distributes topic partitions among workers in the `job-workers` consumer group.

## Core Design Decisions

- PostgreSQL is the source of truth for job lifecycle state.
- Kafka decouples job acceptance from execution and provides durable work distribution.
- `common` contains only contracts shared across services, avoiding direct service-to-service class dependencies.
- Spring Data JPA handles persistence while Flyway owns schema creation.
- A `JobHandler` strategy interface keeps job-type behavior separate from Kafka consumption.
- `JobDispatcher` maps a message type to the correct handler without a growing switch statement.
- Spring application events publish Kafka messages only after the database transaction commits.
- Docker images use multi-stage builds and run as non-root user `10001`.

Redis is connected to the worker and included in health checks. It is intentionally not the source of truth for job state; future uses could include rate limiting, short-lived coordination, or caching.

## Data Model

Jobs are stored in the `jobs` table.

| Column | Purpose |
| --- | --- |
| `id` | UUID primary key |
| `type` | Handler type such as `SEND_EMAIL` |
| `payload` | Flexible JSONB job input |
| `status` | Current lifecycle state |
| `attempts` | Failed processing attempts |
| `max_attempts` | Maximum attempts before dead-lettering |
| `scheduled_at` | Earliest execution time |
| `started_at` | Time processing began |
| `completed_at` | Terminal completion time |
| `last_error` | Most recent processing error |
| `idempotency_key` | Optional unique submission key |
| `version` | Optimistic-lock version |
| `created_at`, `updated_at` | Audit timestamps |

Supported states are `PENDING`, `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`, `DEAD_LETTERED`, and `CANCELLED`.

The `(status, scheduled_at)` index supports polling due jobs, while the unique idempotency-key constraint provides final duplicate protection.

## Reliability

Kafka consumption uses record-level acknowledgement with auto-commit disabled. A record is committed only after its listener returns successfully, giving at-least-once delivery.

Failed jobs use a fixed backoff of two seconds with two retries after the initial attempt. Once attempts are exhausted, the record is published to `jobs.v1.DLT` and the database state becomes `DEAD_LETTERED`.

Job submission publishes its Kafka event after the database transaction commits. This prevents workers from seeing a job that was rolled back, although a production-grade transactional outbox would provide stronger protection against a process crash between database commit and Kafka publication.

## Concurrency

The `jobs.v1` topic has three partitions. Up to three workers in the same consumer group can actively consume in parallel; additional workers remain available for failover.

Scheduled jobs are claimed with PostgreSQL:

```sql
FOR UPDATE SKIP LOCKED
```

This allows multiple scheduler instances to claim different rows without waiting on or duplicating work already locked by another instance.

Idempotent submission has two layers:

1. The API first queries by normalized idempotency key.
2. PostgreSQL enforces a unique constraint for concurrent requests that race between the query and insert.

The losing request catches the constraint violation after its transaction rolls back and returns the job created by the winning request.

## Failure Scenarios

| Scenario | Current behavior |
| --- | --- |
| Worker crashes before offset commit | Kafka reassigns the partition and redelivers the record |
| Worker crashes after database success but before offset commit | Redelivery sees a terminal job and skips duplicate execution |
| Worker crashes after an external side effect but before database success | The handler may run again; external integrations should use `jobId` as an idempotency key |
| Handler throws a transient exception | Kafka retries according to the fixed backoff policy |
| Attempts are exhausted | Job becomes `DEAD_LETTERED` and the message goes to `jobs.v1.DLT` |
| One scheduler instance locks a due job | Other schedulers skip that row and claim different work |
| Two clients submit the same idempotency key | Both receive the same persisted job |
| A container exits unexpectedly | Compose restarts it with `restart: unless-stopped` |

## Observability

Spring Boot Actuator exposes:

- API health and metrics on port `8080`
- worker health and metrics on container port `8081`
- PostgreSQL and Redis health details
- JVM, HTTP, Kafka consumer, connection-pool, and repository metrics

The worker records `jobs.processed` with the outcome tags `succeeded`, `retryable_failure`, `dead_lettered`, and `skipped`.

Example:

```bash
curl "http://localhost:WORKER_PORT/actuator/metrics/jobs.processed?tag=outcome:succeeded"
```

## Benchmarks

No formal throughput or latency benchmark has been claimed yet. Functional testing verified three workers receiving one Kafka partition each, but that is a concurrency validation rather than a performance benchmark.

A useful benchmark should record:

- job-submission latency at p50, p95, and p99
- jobs processed per second
- end-to-end queue latency
- Kafka consumer lag
- PostgreSQL connection and query saturation
- results for one, two, and three worker replicas

The workload, payload size, machine resources, warm-up period, and test duration must be recorded alongside any future results.

## Trade-offs

- At-least-once delivery is resilient but requires idempotent handlers.
- PostgreSQL polling makes scheduled jobs easy to inspect but adds periodic database load.
- `FOR UPDATE SKIP LOCKED` is effective but ties the scheduler query to PostgreSQL.
- JSONB supports varied payloads but shifts job-type validation into handlers.
- A single Kafka broker and replication factor of one suit local development, not production high availability.
- Exposing Actuator details is convenient locally but should be secured in production.
- The current after-commit publisher is simpler than an outbox but cannot atomically commit PostgreSQL and Kafka together.

## Testing

Run the test suite with Java 21:

```bash
export JAVA_HOME=/usr/local/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"

mvn clean test
```

The automated `JobServiceTest` verifies the concurrent idempotency conflict path. Runtime smoke tests have also covered:

- immediate and scheduled job processing
- retry exhaustion and DLT publication
- duplicate idempotency requests
- Actuator health and custom metrics
- three-worker Kafka partition assignment
- end-to-end processing inside Docker Compose

Future tests should use Testcontainers for repeatable PostgreSQL, Kafka, and Redis integration coverage.

## Running Locally

Prerequisites are Docker Desktop, plus Java 21 and Maven only when running tests outside Docker.

Start all services:

```bash
docker compose up -d --build
docker compose ps
```

Submit a job:

```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "SEND_EMAIL",
    "payload": {
      "to": "student@example.com",
      "subject": "Local job test"
    },
    "maxAttempts": 3,
    "idempotencyKey": "readme-example-001"
  }'
```

Query the returned ID:

```bash
curl http://localhost:8080/api/jobs/JOB_ID
```

Scale to three workers:

```bash
docker compose up -d --scale job-worker=3
docker compose ps job-worker
```

Discover each worker's dynamically assigned host port:

```bash
docker compose port --index 1 job-worker 8081
docker compose port --index 2 job-worker 8081
docker compose port --index 3 job-worker 8081
```

List Kafka topics:

```bash
docker compose exec kafka \
  kafka-topics --bootstrap-server kafka:9092 --list
```

Consume dead-letter messages:

```bash
docker compose exec kafka \
  kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic jobs.v1.DLT \
  --from-beginning
```

Inspect logs:

```bash
docker compose logs -f job-api
docker compose logs -f job-worker
```

Stop while preserving PostgreSQL data:

```bash
docker compose down
```

Delete containers and PostgreSQL data:

```bash
docker compose down -v
```

The `-v` option permanently deletes the local database volume.

## What I Learned

- how Maven coordinates a multi-module Java project
- how Spring creates beans and repository implementations
- how JPA maps entities and transactions to PostgreSQL
- how Kafka topics, partitions, offsets, and consumer groups distribute work
- how retries and dead-letter topics handle failures
- how idempotency differs from exactly-once execution
- how optimistic and pessimistic locking solve different concurrency problems
- how Actuator and Micrometer expose runtime behavior
- how multi-stage Docker builds package Java services
- how container DNS differs from host `localhost`

## Future Work

- implement a transactional outbox for reliable database-to-Kafka publication
- add idempotency support to external job side effects
- add Testcontainers integration and end-to-end tests
- export Prometheus metrics and build Grafana dashboards
- add distributed tracing, correlation IDs, and structured JSON logging
- secure API and Actuator endpoints
- add graceful-shutdown tests
- benchmark throughput and latency with reproducible workloads
- deploy with Kubernetes health probes, autoscaling, and managed infrastructure
- replace the single local Kafka broker with a replicated production cluster
