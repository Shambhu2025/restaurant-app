# Restaurant Ordering API — Impactis Project (Milestones 1 & 2)

A RESTful backend for a restaurant ordering platform, built with Spring Boot and PostgreSQL, accelerated with a Redis caching layer, and containerized with Docker.

## Overview

This project delivers a complete, tested REST API covering restaurant management, menus, users, and order placement. 
Following the first milestone, Milestone 2 introduces comprehensive performance optimizations, including resource-state filtering, database indexing on foreign keys, N+1 query resolutions, and a robust Redis caching layer. The system maintains strict request validation, centralized error handling, and automated test coverage above 90%.

## Tech Stack

- Java 25 · Spring Boot 4.1.1 (Web, Data JPA, Validation, Security, Data Redis)
- PostgreSQL 16 & Redis 7.2 (Docker Compose)
- Maven (via Maven Wrapper)
- Lombok (Boilerplate reduction for Entities/DTOs)
- JUnit 5 · Mockito · AssertJ · MockMvc
- JaCoCo (90% coverage enforced)
- Grafana k6 (load testing)

## Endpoints & Caching

| Method | Path | Description | Cache Status |
|---|---|---|---|
| GET | `/restaurants` | List all active restaurants | 🟢 Cached (10m TTL) |
| GET | `/restaurants/{id}` | Get a single restaurant | |
| POST | `/restaurants` | Create a restaurant | Evicts `/restaurants` cache |
| POST | `/restaurants/{id}/menu-items` | Add a menu item | Evicts specific menu cache |
| GET | `/menu?restaurantId=` | Get a restaurant's available menu | 🟢 Cached (5m TTL) |
| POST | `/order` | Place an order | |
| POST | `/users` | Create a user | |
| GET | `/actuator/cachemetrics` | Custom cache hit/miss observability | New in M2 |

## Performance Improvements (Milestone 2)

- **Redis Caching:** Applied to the two most frequently read endpoints, reducing average response time by ~140x (from 2.02s to 14.4ms) and increasing throughput by ~34x under k6 load testing.
- **Resilience:** If Redis is unavailable, requests transparently fall back to PostgreSQL, ensuring 100% uptime with zero failed requests.
- **Database Optimizations:** Added explicit indexes on frequent lookup columns and enforced `FetchType.LAZY` on `@ManyToOne` relationships to eliminate N+1 queries.
- **Resource-State Filtering:** Active/Available states are now strictly enforced at the database query and validation levels, rejecting invalid states with a `409 Conflict`.

## Running Locally

```bash
# 1. Start PostgreSQL and Redis
docker compose up -d

# 2. Run the app
./mvnw spring-boot:run

# 3. Run tests (52 tests total)
./mvnw test
```
## Full Report

For architecture details, design decisions, cache TTL policies, and k6 load testing results, see the full milestone reports:

📄 [First Milestone Report (PDF)](docs/Impactis_Project_First_Milestone_Report.pdf)
📄 [Second Milestone Report (PDF)](docs/Impactis_Project_Second_Milestone_Report.pdf)