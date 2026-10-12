# Restaurant Ordering API — Impactis Project

A RESTful backend for a restaurant ordering platform, built with Spring Boot and PostgreSQL, accelerated with a Redis caching layer, and scaled with Apache Kafka for asynchronous event-driven processing.

## Overview

This project delivers a complete, tested REST API covering restaurant management, menus, users, and order placement. The architecture evolved across three milestones:
*   **Milestone 1:** Core layered architecture, request validation, centralized error handling, and 90%+ test coverage.
*   **Milestone 2:** Performance optimizations including resource-state filtering, database indexing, and a Redis caching layer with fallback resilience.
*   **Milestone 3:** Asynchronous order processing via Apache Kafka, horizontal scaling, exponential backoff retries, and schema management with Flyway.

## Tech Stack

*   **Language & Framework:** Java 25 · Spring Boot 4.1.1 (Web, Data JPA, Validation, Security, Data Redis)
*   **Database & Migrations:** PostgreSQL 16 · Flyway
*   **Caching & Messaging:** Redis 7.2 · Apache Kafka & Zookeeper (Confluent 7.6.0)
*   **Infrastructure:** Docker Compose
*   **Testing:** JUnit 5 · Mockito · AssertJ · JaCoCo (90% coverage enforced) · Grafana k6
*   **Documentation:** OpenAPI / Swagger UI

## Endpoints & Caching

| Method | Path | Description | Notes |
|---|---|---|---|
| GET | `/restaurants` | List all active restaurants | 🟢 Cached (10m TTL) |
| GET | `/restaurants/{id}` | Get a single restaurant | |
| POST | `/restaurants` | Create a restaurant | Evicts `/restaurants` cache |
| POST | `/restaurants/{id}/menu-items` | Add a menu item | Evicts specific menu cache |
| GET | `/menu?restaurantId=` | Get a restaurant's available menu | 🟢 Cached (5m TTL) |
| POST | `/order` | Place an order | 🟡 Async: Returns `202 Accepted` |
| GET | `/order/{id}` | Check order status | Returns `PENDING`, `CONFIRMED`, or `FAILED` |
| POST | `/users` | Create a user | |
| GET | `/actuator/cachemetrics` | Custom cache hit/miss observability | |

## Event-Driven Architecture (Milestone 3)

*   **Non-Blocking Order Processing:** The `POST /order` endpoint performs immediate existence checks, saves the order as `PENDING` with an idempotency key, publishes an `OrderEvent`, and returns a `202 Accepted` in ~20 ms.
*   **Kafka Consumers:** A background consumer group (`order-processing-group`) processes messages across 3 partitions (keyed by `restaurantId` for ordering guarantees), executing business validation, pricing, and database updates in a single transaction.
*   **Resilience & DLT:** Processing failures trigger exponential backoff retries (1s up to 7s). Exhausted retries are routed to a Dead Letter Topic (`order-events.DLT`), where a separate consumer safely marks the order as `FAILED` to prevent endless loops.
*   **Performance:** Under k6 load testing (up to 120 orders/sec), the API successfully accepted 2,699 orders with a p95 response time of 34.0 ms, zero failed HTTP requests, and zero consumer lag upon completion.

## Performance & Developer Experience

*   **Redis Caching:** Applied to the most frequently read endpoints, reducing average response times by ~140x and increasing throughput by ~34x under load. Transparent fallback to PostgreSQL ensures 100% uptime if Redis is unavailable.
*   **Database Optimizations:** Explicit indexes on foreign keys, `FetchType.LAZY` enforcement to eliminate N+1 queries, and resource-state filtering (Active/Available) enforced at the query level.
*   **Versioned Migrations:** Flyway tracks and applies schema changes (V1 through V5) automatically on application startup.
*   **Interactive Docs:** Auto-generated Swagger UI groups endpoints logically and exposes DTO validation schemas dynamically.

## Running Locally

```bash
# 1. Start PostgreSQL, Redis, Kafka, and Zookeeper
docker compose up -d

# 2. Run the app (Flyway applies migrations automatically)
./mvnw spring-boot:run

# 3. Run tests (80 tests total)
./mvnw test
```
Note: Access Swagger UI at http://localhost:8080/swagger-ui.html while the application is running.

## Full Reports

For architecture details, cache TTL policies, Kafka partition strategies, and full k6 load testing results, see the detailed milestone reports:

📄 [First Milestone Report (PDF)](docs/Impactis_Project_First_Milestone_Report.pdf)

📄 [Second Milestone Report (PDF)](docs/Impactis_Project_Second_Milestone_Report.pdf)

📄 [Third Milestone Report (PDF)](docs/Impactis_Project_Third_Milestone_Report.pdf)

