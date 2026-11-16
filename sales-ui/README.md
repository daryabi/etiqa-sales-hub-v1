# Etiqa Sales Hub — Web Frontend (`sales-ui`)

React 19 and TypeScript frontend for **Etiqa Sales Hub**. Provides the login and staff sign-up screen, KPI summary cards, customer directory, book catalog with `+10 Stock` restocking and pipe-delimited bulk file upload, POS sale creation, PDF sales report downloads, and the admin audit log viewer.

## Prerequisites

- **Node.js 22** (with `npm`)
- **Backend API (`sales-api`)** running on `http://localhost:8085`

## Tech Stack

- **Framework:** React 19, TypeScript, Vite
- **Styling:** Tailwind CSS 4
- **Data & Tables:** TanStack Query and TanStack Table
- **Code Quality:** oxlint and TypeScript (`tsc`)
- **Production Serving:** Nginx (listens on port `8080` inside the Docker container, mapped to host port `5174` in `docker-compose.yml`)

## Running Locally

1. **Install dependencies:**
   ```bash
   npm ci
   ```

2. **Start the Vite development server:**
   ```bash
   npm run dev
   ```

3. **Open `http://localhost:5174` in your browser.**
   - In local development, the app calls `http://localhost:8085/api/v1` directly (the default `VITE_API_BASE_URL` fallback) using backend CORS.
   - In Docker builds, `VITE_API_BASE_URL` is set to `/api/v1`, and Nginx proxies `/api` requests to `sales-api:8085`.

## Linting, Type-Checking & Production Build

```bash
npm run lint
npx tsc --noEmit
npm run build
```

The production build outputs static assets to `dist/`.

Run the unit tests with `npm test` (Vitest with jsdom). They cover the HTTP client (`readJson`, `throwProblem`, the 401 handling), Malaysia-time date formatting, `usePagedTable`, and the `DataTable` error and empty states. CI runs them on every push.

## Folder Structure (`src/`)

Each feature owns its screen, API calls, types, and constants. Shared building blocks live outside `features/`.

```
src/
├── App.tsx                 wraps the app in AuthProvider and AuthGate
├── app/                    app shell: AuthGate, AuthenticatedApp (top bar, KPI cards, tabs), TabButton
├── features/
│   ├── auth/               Login screen, AuthContext, authApi, types, form limits
│   ├── customers/          CustomersTab, customersApi, types, empty form
│   ├── products/           ProductsTab (catalog, +10 Stock, bulk upload), productsApi, types, constants
│   ├── sales/              SalesTab (POS), salesApi, types, lookup query
│   ├── reports/            ReportsTab (PDF download), reportsApi
│   └── audit/              AuditLogTab, auditApi, types, filter options
├── api/httpClient.ts       shared fetch helpers: auth header, JSON envelope, 401 handling, paging params
├── components/ui/          reusable pieces: Button, Card, DataTable, Form, Input, Select, Badge, KpiCard, AlertBanner
├── hooks/                  usePagedTable, useDebouncedValue, useApiMutation, useFormFields
├── notifications/          NotificationProvider + useNotify (success and error banners)
├── constants/              shared values: API base URL, session storage keys, query keys, page size, locale, currency
├── types/                  shared types: Page and PageQuery
└── utils/                  formatMoney (MYR), formatDateTime and businessTimezone (the timezone comes from the login response), orDash
```

A feature can use another feature's API module when it needs to. For example, the sales screen loads customers and books for its dropdowns through `customersApi` and `productsApi`.
