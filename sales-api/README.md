# Etiqa Sales Hub — Backend API (`sales-api`)

Spring Boot 3.5 REST API for **Etiqa Sales Hub**. Handles JWT authentication, customer and product management, pipe-delimited bulk imports, sales checkout with stock locking, JasperReports PDF generation, Redis caching, Kafka events, and audit logging.

## Prerequisites

- **Java 21 (JDK 21)**
- **Docker** (for MySQL, Redis, Kafka, and Testcontainers)
- **Maven Wrapper (`./mvnw`)** — included in this folder; no global Maven install required

## Tech Stack

- **Core:** Java 21, Spring Boot 3.5, Spring Web MVC, Spring Data JPA, Spring Security (JWT)
- **Database:** MySQL 8.4 initialized via `spring.sql.init` scripts and validated by Hibernate (`ddl-auto: validate`)
- **Cache & Messaging:** Redis 7 and Apache Kafka 3.9 (`sales.recorded.v1` + `sales.recorded.v1.DLT`)
- **Reporting & Docs:** JasperReports 7 (`SalesReportService` + `src/main/resources/reports/sales_report.jrxml`) and springdoc OpenAPI
- **Quality & Testing:** JUnit 5, Mockito, Testcontainers, Awaitility, JaCoCo (85% line / 75% branch gate), SpotBugs, and PMD

## Running Locally

1. **Start MySQL and Redis from the project root:**
   ```bash
   cd ..
   docker compose up -d mysql redis
   ```

2. **Load `.env` from the project root and start the API:**
   ```bash
   set -a; source .env; set +a
   cd sales-api
   ./mvnw spring-boot:run
   ```

3. **Verify endpoints:**
   - **Health Check:** http://localhost:8085/actuator/health
   - **Swagger UI:** http://localhost:8085/swagger-ui.html
   - **API Base:** http://localhost:8085/api/v1

*(Note: In `docker-compose.yml`, Kafka advertises `PLAINTEXT://kafka:9092` for the Docker network. When running `sales-api` directly on your host machine rather than inside Docker, `kafka` does not resolve; sales still succeed, while full Kafka integration runs inside Docker Compose and in `./mvnw clean verify` via Testcontainers.)*

## Running Tests & Quality Gates

```bash
./mvnw clean verify
```

This runs all unit and Testcontainers integration tests, SpotBugs, PMD, and the JaCoCo coverage gate (`85%` line / `75%` branch). The HTML coverage report is generated at `target/site/jacoco/index.html`.

## Package Structure (`com.etiqa.sales`)

- `audit/` — transactional audit logging support
- `config/` — configuration for Spring Security, Redis cache, `KafkaConfig` (topics, retry, and `.DLT` dead-letter routing), PDF HTTP message converter, and OpenAPI
- `controller/` — REST controllers under `/api/v1` (`AuthController`, `CustomerController`, `ProductController`, `SaleController`, `ReportController`, `AuditLogController`)
- `domain/` — JPA entities (`Customer`, `Product` with `@Version` optimistic locking, `Sale`, `AuditLog`, `UserAccount`)
- `dto/` — request/response records and the `ApiResponse<T>` envelope
- `event/` — domain events (`SaleRecordedEvent`) published after transaction commit
- `exception/` — `GlobalExceptionHandler` producing RFC 7807 `application/problem+json` errors and `409 Conflict` responses
- `filter/` — `RequestResponseLoggingFilter` (logs method, URI, status, duration, and masked bodies before security)
- `messaging/` — Kafka event publisher and low-stock consumer
- `repository/` — Spring Data JPA repositories
- `security/` — JWT filter, token service, and `DatabaseUserDetailsService`
- `service/` — business logic, stock reconciliation, bulk import parser, and `SalesReportService` (JasperReports PDF)
- `util/` — shared helper utilities

## Database Scripts (`src/main/resources/db/`)

Executed by Spring Boot on startup in foreign-key dependency order:
- `customers.sql` — `customers` table with unique office and personal email indexes
- `products.sql` — `products` table with `version` column, stock/price checks, and active/stock index
- `sales.sql` — `sales` table with foreign keys to `customers` and `products` plus `idx_sales_date`
- `audit_logs.sql` — `audit_log` table with timestamp and entity indexes
- `users.sql` — `users` table with unique username constraint and role check
- `seed_users.sql` — idempotent `INSERT IGNORE` seed for `admin` (`admin123`) and `staff` (`staff123`)

## Configuration (`application.yml`)

- **`app.sales.*`:** configures the Kafka topic name (`sales.recorded.v1`), low-stock alert threshold (`10`), and JWT expiration (`24h`).
- **`METRICS_PASSWORD` (optional):** enables HTTP Basic authentication (`prometheus` user) on `/actuator/prometheus`; when empty, the endpoint rejects all requests.