# mini-bank-api

REST API for bank accounts and money transfers, built with **Java 17, Spring Boot 3, Spring Data JPA, PostgreSQL and Flyway**.
Focus: correct money movement under concurrency — atomic transfers, row-level locking, idempotent requests.

## Features

- Open accounts, deposit, block, get balance
- Transfers between accounts (`POST /api/v1/transfers`)
  - `@Transactional`: debit, credit and the transaction record commit together or not at all
  - `SELECT ... FOR UPDATE` row locks, always taken in ascending id order → no deadlocks
  - `Idempotency-Key` header: a retried request is applied only once
  - Rejects insufficient funds, blocked accounts, currency mismatch
- Paginated transaction history
- Validation (Bean Validation) and RFC 7807 error responses with stable error codes
- Schema managed by Flyway migrations (`ddl-auto: validate`)
- OpenAPI / Swagger UI, Actuator health check
- Tests: JUnit 5 + Mockito unit tests, `@WebMvcTest` controller tests,
  Testcontainers integration tests against real PostgreSQL (incl. 100 concurrent transfers)
- Docker multi-stage image, Docker Compose, GitHub Actions CI

## Run

```bash
docker compose up --build          # app on :8080, PostgreSQL on :5432
# or: docker compose up -d db && mvn spring-boot:run
```

Swagger UI: http://localhost:8080/swagger-ui.html

```bash
curl -X POST localhost:8080/api/v1/accounts -H 'Content-Type: application/json' \
     -d '{"ownerName":"Alice","currency":"KZT"}'
curl -X POST localhost:8080/api/v1/accounts/1/deposit -H 'Content-Type: application/json' -d '{"amount":1000}'
curl -X POST localhost:8080/api/v1/accounts -H 'Content-Type: application/json' \
     -d '{"ownerName":"Bob","currency":"KZT"}'
curl -X POST localhost:8080/api/v1/transfers -H 'Content-Type: application/json' \
     -H 'Idempotency-Key: 5f1c-demo' -d '{"fromAccountId":1,"toAccountId":2,"amount":250}'
curl 'localhost:8080/api/v1/accounts/1/transactions?page=0&size=20'
```

## Test

```bash
mvn test        # integration tests run automatically when Docker is available
```

## Structure

```
account/   Account entity (business rules), repository, service, controller, DTOs
transfer/  Transaction entity, TransferService (locking + idempotency), controller
common/    exceptions + global exception handler
db/migration/  Flyway SQL
```

## Ideas for next steps

- Spring Security + JWT (customer can only see own accounts)
- Currency conversion with exchange rates
- Outbox table + Kafka event on every transfer
- Rate limiting for transfer endpoint
