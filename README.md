# Distributed Job Processing Platform

A lean backend engineering project focused on distributed job processing using Java, PostgreSQL, Kafka, Redis, and Docker.

## Objective

Build a production-style platform that can:

- accept jobs through an API
- persist job state
- publish work to a queue
- process jobs with workers
- retry transient failures
- move permanent failures to a dead-letter queue
- support scheduling and basic concurrency
- expose idempotent job submission

## Tech stack

- Java 21
- Spring Boot 3
- PostgreSQL
- Kafka
- Redis
- Docker + Docker Compose
- Maven

## Planned phases

1. Project initialization and infrastructure setup
2. Job API and persistence layer
3. Kafka producer and consumer integration
4. Worker processing, retries, and DLQ behavior
5. Scheduling and concurrency controls
6. Idempotency and observability
7. Containerized local runbook and final polish

## Local infrastructure

Run:

```bash
docker compose up -d
```

This starts:

- PostgreSQL on localhost:5432
- Redis on localhost:6379
- Kafka on localhost:9092
- Zookeeper on localhost:2181

## Module layout

- `common` shared domain and utilities
- `job-api` REST API for accepting and querying jobs
- `job-worker` asynchronous worker consumer

## Project timeline

This project is intentionally scoped to take several focused hours, with checkpoints after each major phase.
