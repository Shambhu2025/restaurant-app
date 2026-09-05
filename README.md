# Restaurant Ordering API — Impactis Project (Milestone 1)

A RESTful backend for a restaurant ordering platform, built with Spring Boot and PostgreSQL, containerized with Docker.

## Overview

This milestone delivers a complete, tested REST API covering restaurant management, menus, users, and order placement — with request validation, centralized error handling, automated test coverage above 90%, and load testing performed with [k6](https://k6.io/).

## Tech Stack

- Java 25 · Spring Boot 4.1.1 (Web, Data JPA, Validation, Security)
- PostgreSQL 16 (Docker)
- Maven (via Maven Wrapper)
- JUnit 5 · Mockito · AssertJ · MockMvc
- JaCoCo (90% coverage enforced)
- Grafana k6 (load testing)

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/restaurants` | List all restaurants |
| GET | `/restaurants/{id}` | Get a single restaurant |
| POST | `/restaurants` | Create a restaurant |
| POST | `/restaurants/{id}/menu-items` | Add a menu item |
| GET | `/menu?restaurantId=` | Get a restaurant's menu |
| POST | `/order` | Place an order |
| POST | `/users` | Create a user |

## Running Locally

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Run the app
./mvnw spring-boot:run

# 3. Run tests
./mvnw test
```

## Full Report

For architecture details, design decisions, test coverage results, and k6 load testing results (burst + stress tests), see the full milestone report:

📄 [First Milestone Report (PDF)](docs/Impactis_Project_First_Milestone_Report.pdf)