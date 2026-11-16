# Etiqa Sales Hub



A full-stack point-of-sale (POS) and inventory management web application built with Spring Boot, React, MySQL, Redis, and Kafka. It lets staff and administrators manage customers, track book inventory, bulk-import products from a pipe-delimited file, record sales with stock locking, download PDF sales reports, and view an audit log of all changes.

## Requirements Coverage

Here is how each requirement from the technical brief is implemented in the codebase:

1. **Customer endpoints (office and personal email)** — `customer/CustomerController`, `customer/CustomerService`, and `customers.sql` (`POST/GET/PUT/PATCH/DELETE /api/v1/customers`).
2. **Product endpoints (`Book_ID`, `Title`, `Price`, `Stock`)** — `product/ProductController`, `product/ProductService`, and `products.sql` (`POST/GET/PUT/PATCH/DELETE`, plus `/restock`).
3. **Sales table and endpoints (`Sale_ID`, `Sale_Date`, `Book_ID`, `SalesQuantity`, `SalesPrice`)** — `sale/SaleController`, `sale/SaleService`, and `sales.sql` (`POST/GET/PUT/PATCH /api/v1/sales`).
4. **Pipe-delimited (`|`) file upload (`MultipartFile`)** — `product/ProductController#importProducts` and `product/ProductImportService` (`POST /api/v1/products/upload`, with `sample-books.txt`).
5. **PDF sales report by date (JasperReports)** — `report/SalesReportController`, `report/SalesReportService`, and `reports/sales_report.jrxml` (`GET /api/v1/reports/sales/pdf`).
6. **RESTful JSON, Swagger, and error handling** — `ApiResponse<T>` envelope, springdoc OpenAPI (`/swagger-ui.html`), and `GlobalExceptionHandler` (RFC 7807 with `correlationId`).
7. **API request and response logging** — `common/web/RequestResponseLoggingFilter` writes a `REQUEST` line (method, path, headers, body) and a `RESPONSE` line (status, duration, body) for every call, with secrets masked, to the console and to `logs/sales-api.log` (`logback-spring.xml`).
8. **Testing and code quality** — JUnit 5, Mockito, Testcontainers, JaCoCo (85% line / 75% branch gate), SpotBugs, PMD, oxlint, and GitHub Actions CI.
9. **Caching, messaging, and containers** — Redis catalog cache, Kafka (`sales.recorded.v1`, `inventory.stock-changed.v1` + `.DLT`), Docker Compose, plus an optional Prometheus/Grafana/ELK stack.
10. **Transactions** — every service method that reads or writes the database is marked `@Transactional`, with `readOnly = true` on reads.
11. **Pagination** — list endpoints return 10 records per page when no `size` is given.
12. **Nested third-party call** — `GET /api/v1/products/isbn/{isbn}` goes client → our API → Open Library (`product/BookLookupService`), with timeouts and clean `404`/`502` errors.
13. **Postman** — `postman/etiqa-sales-hub.postman_collection.json` covers every endpoint.
14. **Microsoft SQL Server** — an optional `mssql` profile runs the same app on SQL Server (`TESTDB`).

## Tech Stack

- **Backend (`sales-api`):** Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA, MySQL 8.4, Redis 7, Kafka 3.9, JasperReports 7
- **Frontend (`sales-ui`):** React 19, TypeScript, Vite, Tailwind CSS 4, TanStack Query and Table, Nginx
- **Testing and Quality:** JUnit 5, Mockito, Testcontainers, Awaitility, JaCoCo, SpotBugs, PMD, oxlint, GitHub Actions
- **Observability (Optional):** Structured ECS JSON logs, Micrometer, Prometheus, Grafana, Filebeat, Elasticsearch, Kibana

## Prerequisites

- **To run with Docker (recommended):** Docker Desktop or Docker Engine with Docker Compose v2.
- **To run locally without Docker for app code:** JDK 21, Node.js 22, and Docker (for MySQL, Redis, Kafka, and Testcontainers).

## Installation and Setup (Docker)

1. **Clone the repository and enter the project folder:**
   ```bash
   git clone https://github.com/daryabi/etiqa-sales-hub.git
   cd etiqa-sales-hub
   ```

2. **Create your local `.env` file from the template:**
   ```bash
   cp .env.example .env
   ```

3. **Generate a random JWT secret and configure `.env`:**
   ```bash
   openssl rand -base64 48
   ```
   Open `.env`, paste the generated string into `JWT_SECRET=`, set your database username (`DB_USER`), and set passwords for `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `REDIS_PASSWORD`, `METRICS_PASSWORD`, and `GRAFANA_ADMIN_PASSWORD`.

4. **Build and start the core stack (5 containers):**
   ```bash
   docker compose up --build -d
   ```

5. **Open the application in your browser:**
   - **Web Application:** http://localhost:5174
   - **REST API Base URL:** http://localhost:8085/api/v1
   - **Swagger UI:** http://localhost:8085/swagger-ui.html
   - **Health Check:** http://localhost:8085/actuator/health

To wipe the database volume and start fresh at any time:
```bash
docker compose down -v && docker compose up -d --build
```

Nginx serves the frontend on port `5174` and proxies `/api` requests to `sales-api:8085` under one origin. MySQL, Redis, and Kafka bind strictly to `127.0.0.1`.

## How to Use the Application

1. **Sign in or create a Staff account**
   Open `http://localhost:5174`. Two demo accounts are created automatically on first startup:
   - **Admin:** username `admin`, password `admin123` (full access, including deleting records and viewing the Audit Log)
   - **Staff:** username `staff`, password `staff123` (can do everything except deleting records and viewing the Audit Log)
   - **Sign Up:** Click the **Sign Up** tab on the login screen to register a new account (username 3–50 characters, password 6–72 characters). Self-registered users always receive the `STAFF` role (any `role` field in the API payload is ignored) and are signed in immediately.

2. **Customers (`Customers` tab)**
   - View, search, and add customers with their first name, last name, office email, and personal email.
   - Updating (`PUT`/`PATCH`) and deleting (`DELETE`, Admin only, blocked if the customer has sales) are available through the REST API and Swagger UI.

3. **Books & Bulk Upload (`Books & Upload` tab)**
   - Add new books with a title, price, and starting stock quantity, or click **+10 Stock** on any row to add 10 units atomically.
   - Use the **Bulk Pipe (`|`) File Upload** panel to upload a `.txt` or `.csv` file (up to 5 MB; see `sample-books.txt`):
     ```text
     bookTitle|bookPrice|bookStockQuantity
     Clean Architecture|39.99|50
     ```
     The header row is optional. Valid rows save immediately, and invalid rows are reported back with their exact line numbers.
   - Full product editing (`PUT`/`PATCH`) and soft deletion (`DELETE`, Admin only) are also available via the API / Swagger UI.

4. **Record Sales (`POS & Sales` tab)**
   - Select a customer, choose an active book, enter the quantity, and submit the sale.
   - Stock is checked and deducted immediately. If multiple buyers race for the last copy, one sale succeeds (`201 Created`) and the others receive a `409 Conflict` error (*Insufficient Stock* or *Concurrent Update Conflict*).
   - Editing an existing sale (`PUT`/`PATCH /api/v1/sales/{id}`) is supported via the API / Swagger UI and automatically reconciles stock while keeping the original unit price unless the book changes.
   - An admin can delete a sale (`DELETE /api/v1/sales/{id}`). Its quantity goes back into stock and the delete is written to the audit log.

5. **Download PDF Sales Reports (`PDF Reports` tab)**
   - Pick a **Start Date** and **End Date** (up to 366 days) and download `sales-report-<start>-<end>.pdf`, generated by JasperReports. Days follow Malaysia time (`Asia/Kuala_Lumpur`, set by `app.timezone`), so a sale at 12:30am counts on the right day, and the PDF header shows the timezone.

6. **Audit Log (`Audit Log` tab — Admin only)**
   - Sign in as `admin` to review all recorded changes (`CUSTOMER`, `PRODUCT`, `SALE`) and `LOW_STOCK_ALERT` events.

## Database Schema

Schema scripts live in individual SQL files under `sales-api/src/main/resources/db/`:
- `customers.sql`
- `products.sql`
- `sales.sql`
- `audit_logs.sql` (creates the `audit_log` table)
- `users.sql`
- `seed_users.sql` (seeds `admin` and `staff` with BCrypt password hashes)

Spring Boot executes these scripts via `spring.sql.init` in foreign-key dependency order before Hibernate validates the entity mappings (`ddl-auto: validate`). Every script uses `CREATE TABLE IF NOT EXISTS` and `INSERT IGNORE`, so restarts preserve existing data. Because no migration table tracks schema versions, changing an existing table requires either a manual `ALTER TABLE` or resetting the database (`docker compose down -v`).

## Running Locally (Without Docker for App Code)

1. **Start MySQL, Redis, and Kafka in Docker:**
   ```bash
   docker compose up -d mysql redis kafka
   ```

2. **Load `.env` and start the Spring Boot API:**
   ```bash
   set -a; source .env; set +a
   cd sales-api && ./mvnw spring-boot:run
   ```

3. **Start the React frontend in a second terminal:**
   ```bash
   cd sales-ui && npm ci && npm run dev
   ```

4. **Open `http://localhost:5174` in your browser.**

**Heads up about Kafka:** inside Docker, Kafka tells every client to connect to `kafka:9092`. That name only works inside Docker. So when you run the API on your own machine, sales still save fine, but the low-stock alerts won't come through. If you want to see the full Kafka flow, run the API with Docker Compose instead. The integration tests cover it too.

## Running on Microsoft SQL Server (Optional)

The default setup uses MySQL (Docker Compose and most tests run on MySQL). To run the API against a local SQL Server database called `TESTDB` instead:

1. Create the database once: `CREATE DATABASE TESTDB;`
2. Start Redis and Kafka: `docker compose up -d redis kafka`
3. Start the API with the `mssql` profile:
   ```bash
   set -a; source .env; set +a
   export MSSQL_USER=sa MSSQL_PASSWORD='your-sa-password'
   cd sales-api && ./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.profiles.active=mssql
   ```

The profile lives in `application-mssql.yml`. It connects to `jdbc:sqlserver://localhost:1433;databaseName=TESTDB;encrypt=true;trustServerCertificate=true` (override with `MSSQL_URL`) and creates the tables from SQL Server scripts in `db/mssql/`. A few things are different on SQL Server:
- The tables use `IDENTITY`, `DATETIME2`, and `BIT`, and the audit payload is `NVARCHAR(MAX)`.
- Customer emails use filtered unique indexes, because a plain `UNIQUE` column in SQL Server allows only one empty value.
- Hibernate's schema check is off for this profile, because the audit payload is mapped as MySQL's `json` type.

`MssqlProfileIntegrationTest` checks this profile on every build. It starts SQL Server 2022 in a container, creates `TESTDB`, boots the app with the `mssql` profile, and checks that sign-in and customers work. It also checks that two customers with no email can coexist while a repeated email is still refused, so the filtered indexes really do their job. The first run downloads the SQL Server image (about 1.5 GB), so it takes a while once.

## Running Tests & Quality Gates

Backend (unit tests, integration tests, SpotBugs, PMD, and the JaCoCo gate of 85% line / 75% branch coverage):

```bash
cd sales-api
./mvnw clean verify
```

Frontend (lint, type check, Vitest unit tests, and production build), from the project root:

```bash
cd sales-ui
npm run lint && npx tsc --noEmit && npm test && npm run build
```

The integration suite spins up real MySQL, Redis, and Kafka containers and tests the API end-to-end with Spring's `RestClient`, verifying:
- Eight people try to buy the last copy of a book at the same time. One gets `201`, the other seven get `409`, nobody gets a server error, and the stock ends at `0`.
- A sale dropping stock below the threshold (`10`), triggering a stock event consumed into a `LOW_STOCK_ALERT` audit entry.
- A malformed payload on `inventory.stock-changed.v1` being routed to `inventory.stock-changed.v1.DLT`.
- The same `Idempotency-Key` sent twice recording only one sale, the same key with a different sale getting `422`, and another user's identical key creating its own sale.
- Importing the same file twice skipping every row the second time.
- A report longer than 366 days being rejected.
- Redis catalog cache eviction after product updates, restocks, and sales.
- Bulk upload error handling (wrong delimiter, invalid rows, empty file, missing file).
- An admin deleting a sale gets `204` and the stock back, while staff get `403`.
- The `mssql` profile booting on a real SQL Server 2022 container with `TESTDB` (see above).

## Observability (Optional)

`sales-api` writes structured **ECS JSON** logs to stdout with a `correlationId` per request. Before anything is logged, `password`, `token`, `secret`, and email fields (such as `emailOffice` and `emailPersonal`) are replaced with `***`. Prometheus metrics are exposed at `/actuator/prometheus` and accept only HTTP Basic auth for username `prometheus` using `METRICS_PASSWORD`.

Every API call produces two log lines that share a `correlationId`:
- `REQUEST POST /api/v1/customers correlationId=… headers={…}` — written as soon as the request arrives, so even a request that hangs shows up. The `Authorization`, `Cookie`, and `X-Api-Key` header values are replaced with `***`.
- `RESPONSE POST /api/v1/customers -> 201 (35 ms) correlationId=… requestBody={…} responseBody={…}` — written when the response is done.

JSON bodies are parsed and any field whose name contains `password`, `token`, `secret`, or `email` is replaced with `***`, at any depth and whatever its type. File uploads are logged as `<multipart: N bytes>`, PDFs as `<binary: N bytes>`, and JSON that can't be parsed as `<unreadable JSON: N chars>`, so file contents and broken payloads never reach the logs.

`logback-spring.xml` sends the logs to the console and to `logs/sales-api.log` (in the container, `/app/logs/sales-api.log`). The file rolls over at 10 MB and keeps 7 days. Set `LOG_FILE` to write somewhere else.

1. **Start Prometheus, Grafana, Elasticsearch, Kibana, and Filebeat alongside the core stack** (needs `METRICS_PASSWORD` and `GRAFANA_ADMIN_PASSWORD` in `.env`):
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.observability.yml up -d
   ```

2. **Open the monitoring dashboards:**
   - **Prometheus:** http://localhost:9090 (scrapes target `etiqa-sales-api`)
   - **Grafana:** http://localhost:3000 (login: `admin`, pre-loaded with the **Etiqa Sales Hub API** dashboard)
   - **Kibana:** http://localhost:5601 (create a data view matching `filebeat-*`)
   - **Elasticsearch:** http://localhost:9200

3. **Stop the full stack:**
   ```bash
   docker compose -f docker-compose.yml -f docker-compose.observability.yml down
   ```

A few things worth knowing:
- Elasticsearch runs with security turned off (`xpack.security.enabled: "false"`). That's fine on your own machine, but don't expose it on a shared network.
- In a live test, the five extra containers used about 2.1 GB of memory together: Elasticsearch about 1 GB, Kibana about 670 MB, Grafana about 245 MB, Filebeat about 150 MB, and Prometheus about 40 MB.
- The API container uses Docker's `json-file` log driver, so Filebeat can read its log file no matter what the Docker default is on your machine.
- There is no Logstash. The API already writes ECS JSON, which is the shape Elasticsearch and Kibana expect, so there is nothing left to parse or reshape. Filebeat just decodes each line and ships it straight to Elasticsearch. That is one less JVM using memory on a laptop. If we ever need to enrich or route logs to more than one place, Logstash can go in between without changing the API.

## API Overview

All endpoints live under `/api/v1` and require `Authorization: Bearer <token>` unless marked public.

- **Success envelope:** `{ "message": "OK", "data": { ... }, "timestamp": "2026-11-15T09:00:00Z" }`
- **Error format:** RFC 7807 `application/problem+json` including a `correlationId`.
- **Pagination and search:** List endpoints accept `page` (0-indexed), `size` (default `10`, max `100`), `sort=field,asc|desc`, and `search`. Without a `size`, you get 10 records per page.

### Endpoints by Resource

- **Auth (Public):**
  - `POST /auth/login` — sign in and receive a JWT token plus the business timezone the UI uses for dates
  - `POST /auth/register` — create a new `STAFF` account and receive a JWT token
- **Customers (Authenticated; `DELETE` is Admin only):**
  - `GET /customers`, `GET /customers/{id}` — list or view customers
  - `POST /customers` — add a customer
  - `PUT /customers/{id}`, `PATCH /customers/{id}` — update customer details
  - `DELETE /customers/{id}` — delete a customer with no sales
- **Products (Read endpoints are Public; write endpoints require Authentication; `DELETE` is Admin only):**
  - `GET /products`, `GET /products/{id}`, `GET /products/summary` — browse catalog and stock totals
  - `GET /products/isbn/{isbn}` — look up a book's title, publisher, date, and page count from Open Library (signed-in users only)
  - `POST /products`, `PUT /products/{id}`, `PATCH /products/{id}` — create or edit products
  - `POST /products/{id}/restock` — add stock atomically
  - `POST /products/upload` — bulk import products from a pipe-delimited file
  - `DELETE /products/{id}` — deactivate a product
- **Sales (Authenticated; `DELETE` is Admin only):**
  - `GET /sales`, `GET /sales/{id}`, `GET /sales/summary` — view sales and revenue totals
  - `POST /sales` — record a new sale and deduct stock
  - `PUT /sales/{id}`, `PATCH /sales/{id}` — update a sale and reconcile stock
  - `DELETE /sales/{id}` — remove a sale (for example a refund or a mistake) and put its quantity back in stock; returns `204`
- **Reports (Authenticated):**
  - `GET /reports/sales/pdf?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD` — download a PDF sales report
- **Audit Log (Admin only):**
  - `GET /audit-logs?entityName=CUSTOMER|PRODUCT|SALE` — view change history

### Postman Collection

Import `postman/etiqa-sales-hub.postman_collection.json` into Postman. Then:
1. Run **Auth → Login (staff)** and **Auth → Login (admin)**. They save the tokens into the collection variables `token` and `adminToken`.
2. Run the other requests in order. Each "Create" request saves the new ID (`customerId`, `bookId`, `saleId`), so the get, update, and delete requests after it just work.

`baseUrl` defaults to `http://localhost:8085/api/v1`. List requests use `page=0&size=10`. For **Import books from pipe file**, pick `sample-books.txt` from the project root. The daily PDF request sets today's Malaysian date for you.

## Design Notes

- **Two people buying the same book:** When a sale comes in, the new stock number is written to the database first, and only then is the sale row added. Because of this order, two buyers of the same book wait their turn instead of blocking each other (a deadlock). The `@Version` column then catches whoever was second and returns `409 Conflict`. Any other locking problem also comes back as `409`, never as a server error. Restocking is one single `UPDATE ... SET stock = stock + n`, so two restocks can't overwrite each other.
- **Sale Edits:** Updating a sale restores the previous quantity to stock before deducting the new quantity and preserves the original unit price unless the book changes.
- **Transactions:** Every service method that touches the database has an explicit `@Transactional`. Reads use `@Transactional(readOnly = true)`. Helpers like `getProductOrThrow` and `deductStock` join the caller's transaction, so a sale, its stock change, and its audit row commit or roll back together.
- **Calling Open Library:** `BookLookupService` checks the ISBN-10/13 check digit before calling out, so typos fail fast with a `400`. It uses its own `RestClient` with a 3 second connect timeout and a 5 second read timeout (`app.book-lookup.*`) and sends `User-Agent: etiqa-sales-hub`. Successful lookups are cached in Redis for 24 hours. If Open Library has no match, we return `404`. If it fails or is too slow, we return `502 External Service Unavailable` instead of hanging or leaking a stack trace.
- **Transactional Audit and After-Commit Kafka:** Audit rows are saved inside the business transaction. Events go to Kafka only after commit: `SaleRecordedEvent` (keyed by sale ID) when a sale is recorded, and `StockLevelChanged` (keyed by book ID, so one book's changes stay in order) whenever stock moves.
- **Who reads `sales.recorded.v1`:** Nothing inside this app, and that's on purpose. It's an outbound feed for other systems, such as finance, a data warehouse, or a loyalty service, so they can react to sales without calling our API or reading our database. The low-stock alert listens to `inventory.stock-changed.v1` instead, because it cares about stock, not sales.
- **Feature boundaries:** The product feature owns stock. `SaleService` asks `ProductService.deductStock` and `returnStock` to change it, and only by the net difference when a sale is edited. The report feature reads sales only through `SaleService.findReportLines`, and renders the PDF after that read transaction has closed.
- **Kafka Retry and Dead-Letter Policy:** `KafkaConfig` declares `sales.recorded.v1`, `inventory.stock-changed.v1`, and `inventory.stock-changed.v1.DLT`. The low-stock consumer reads stock events and raises an alert only when stock goes from 10 or more to below 10, so you get one alert per drop, not one per sale. A restock above 10 re-arms it. Failed events retry twice (1 second apart) before moving to `.DLT`; invalid JSON goes straight to `.DLT`.
- **Redis Cache and Soft Deletes:** Product pages are cached in Redis per page/search query, evicted after commit on product or stock changes, and fall back to MySQL if Redis is unreachable. Products referenced by sales are deactivated rather than hard-deleted; customers with sales cannot be deleted (`409 Conflict`).
- **Customer emails:** Office and personal email must be different, and each must be unused by other customers. Create, update, and patch all apply the same check and return the same `400`. If two requests race past the check, the database's unique index catches the second one and it gets the same `400`, not a `409`.
- **Safe retries for sales:** `POST /sales` accepts an `Idempotency-Key` header. Keys belong to the user who sent them (`idemp:{username}:{key}` in Redis), so two cashiers can never collide. While the sale is running the key is only locked for 2 minutes, so a crashed request can't block that key for long. Once the sale succeeds, the result is kept for 24 hours. Along with the key we store a SHA-256 hash of the sale (customer, book, quantity). A repeat with the same sale gets the original sale back. A repeat while the first is still running gets `409 Duplicate Request`. The same key with a different sale gets `422 Idempotency Key Reused`, because quietly returning the old sale would hide a client bug. The web form keeps one key until the sale goes through, so a double click can't sell twice. If Redis is down, the sale is still recorded.
- **Bulk import:** The file is parsed before any database work starts. Titles that repeat in the file or already exist in the catalog (case-insensitive) are skipped and reported per line as "already in catalog", so uploading the same file twice is safe.
- **Report limits:** A PDF report can cover at most 366 days (`app.max-report-days`).
- **Centralized Messages:** User-facing strings live in `messages.properties` and are verified against the `MessageKey` enum (in `i18n/`) by a test.

## Environment Variables

Set these variables in `.env`:
- **`JWT_SECRET`:** Base64 HMAC secret (at least 32 bytes; required on startup).
- **`DB_USER`, `DB_PASSWORD`, `DB_ROOT_PASSWORD`:** MySQL credentials for `etiqa_sales_db`.
- **`REDIS_PASSWORD`:** Redis password.
- **`METRICS_PASSWORD`:** Password for the `prometheus` scrape account (`/actuator/prometheus` rejects all requests when empty).
- **`GRAFANA_ADMIN_PASSWORD`:** Grafana `admin` password (used by `docker-compose.observability.yml`).

## Known Limitations

- Changing user passwords or roles (`STAFF` vs. `ADMIN`) is done directly in the `users` table; there is no user-admin screen in the UI yet.
- Editing/deleting customers, editing/soft-deleting books, and editing sales are exposed via the REST API (Swagger UI), while the web UI focuses on creation, restocking (`+10 Stock`), bulk upload, and reporting.
- Sale form dropdowns load the first 100 customers and first 100 books.
- JWTs are stored in `localStorage`, protected by a strict Content-Security-Policy served by Nginx.
- Kafka sale events are sent right after the database commit, on a best-effort basis. If the broker is down at that moment, the sale is still saved but its event is lost (the failure is logged). A transactional outbox table would guarantee delivery.