# Account Aggregator Integration

Consent-driven retrieval of customer financial data from FIPs (via Digio's AA sandbox),
normalized into a canonical schema for internal FIU services (underwriting, risk analytics,
reconciliation).

## Architecture

```
Angular UI  ──▶  Spring Boot API  ──▶  Digio (AA gateway)  ──▶  FIP (bank/NBFC/etc.)
                      │
                      ▼
                 PostgreSQL
        (consent records, normalized accounts/
         transactions/loans, audit log)
```

- **Consent lifecycle**: `POST /api/v1/consents` creates a consent locally and registers it
  with Digio; the customer approves it via the returned `redirectUrl`; status is tracked via
  polling (`GET /api/v1/consents/{id}/status`) and via Digio's webhook
  (`POST /api/v1/webhooks/digio`).
- **Data retrieval**: once a consent is `ACTIVE`, `POST /api/v1/internal/fi-data/fetch`
  starts an FI data session with Digio. When Digio signals the data is ready (webhook event
  `FI_DATA_READY`, or the manual sync fallback endpoint), the payload is fetched and normalized
  into `account` / `transaction` / `loan_account` rows.
- **Normalization**: `DataNormalizationService` is the single translation point between
  whatever shape Digio/the FIP returns and our canonical schema — if a FIP payload quirk shows
  up, that's the one file to fix.
- **Compliance/audit**: every consent and fetch state transition is written to `audit_log`,
  correlated by a request-scoped correlation ID (`X-Correlation-Id` header, propagated through
  logs via MDC).

## Project layout

```
backend/    Spring Boot service (Java 17, Maven)
frontend/   Angular UI (consent initiation + status)
docker-compose.yml   Local Postgres + backend
```

## Prerequisites

- Java 17, Maven (or use the Docker path below and skip local installs)
- Node.js 18+ and npm, for the Angular frontend
- PostgreSQL 14+ (or Docker)
- Your Digio AA **sandbox** credentials (username, password, template ID) from onboarding

## 1. Configure credentials (do this first)

**Never commit real credentials or paste them into a chat/ticket.** Copy the example env file
and fill in your own values locally:

```bash
cd backend
cp .env.example .env
# edit .env and set DIGIO_USERNAME, DIGIO_PASSWORD, DIGIO_TEMPLATE_ID, DB_*, INTERNAL_API_KEY
```

`.env` is already in `.gitignore`. When running via Docker Compose, `docker-compose.yml` reads
the same variable names from your shell environment or a `.env` file at the repo root — either
`export`  them or `cp backend/.env.example .env` at the project root.

Confirm the exact Digio host/paths against your onboarding docs — `DIGIO_BASE_URL` and the
endpoint paths in `DigioClientService` are set from the standard AA API shape but should be
verified against the reference you were sent.

## 2. Run with Docker Compose (fastest path)

```bash
export $(cat backend/.env | xargs)   # or `cp backend/.env .env` at repo root
docker compose up --build
```

This starts Postgres (with the schema applied automatically via Flyway on backend startup) and
the backend on `http://localhost:8080`.

## 3. Run locally without Docker

```bash
# Postgres running locally, database `aa_service` created and credentials matching .env
cd backend
export $(cat .env | xargs)
mvn spring-boot:run
```

Flyway applies `V0__extensions.sql` and `V1__init_schema.sql` automatically on startup.

## 4. Run the frontend

```bash
cd frontend
npm install
npm start
```

Opens on `http://localhost:4200`, calling the backend at `http://localhost:8080/api/v1`
(see `src/environments/environment.ts`).

## 5. Run backend tests

```bash
cd backend
mvn test
```

Includes unit tests for `DataNormalizationService` covering the FIP-payload-to-canonical-model
mapping, including malformed amount/date handling.

## API surface

| Method | Path | Caller | Purpose |
|---|---|---|---|
| POST | `/api/v1/consents` | Angular UI | Create consent, get redirect URL for approval |
| GET | `/api/v1/consents/{id}/status` | Angular UI | Poll consent status |
| GET | `/api/v1/internal/consents?customerRef=` | FIU services (API-key) | List a customer's consents |
| POST | `/api/v1/internal/fi-data/fetch` | FIU services (API-key) | Start an FI data session |
| GET | `/api/v1/internal/fi-data/{id}/accounts` | FIU services (API-key) | Read normalized accounts |
| POST | `/api/v1/internal/fi-data/sessions/{sessionId}/sync` | FIU services (API-key) | Manual fetch+normalize fallback |
| POST | `/api/v1/webhooks/digio` | Digio | Async consent/FI-data notifications |

Business/internal endpoints require header `X-Internal-Api-Key: <INTERNAL_API_KEY>`. The
consent-creation/status endpoints called by the Angular UI are intentionally NOT API-key gated
(a browser can't hold a server secret) — before production, replace the open access there with
real end-user auth (session/JWT) so a caller can only see their own consents; see the comment
in `ConsentController` and `SecurityConfig` for where to wire that in.

Swagger/OpenAPI UI: `http://localhost:8080/swagger-ui/index.html` once the backend is running.

## Security & compliance notes

- Credentials are only ever read from environment variables — nothing is hardcoded, nothing
  should be committed.
- Raw FIP payloads are **not** persisted — only normalized fields, and account numbers are
  stored masked (as delivered by the AA ecosystem, e.g. `XXXXXXXX1234`).
- Every consent/fetch state change and webhook delivery is written to `audit_log` with a
  correlation ID for traceability.
- The Digio webhook endpoint verifies an HMAC-SHA256 signature (`X-Digio-Signature`) using
  `DIGIO_WEBHOOK_SECRET` when configured — confirm the header name/scheme against Digio's
  webhook docs and adjust `WebhookController.verifySignature()` if it differs.
- Outbound Digio calls use retry with exponential backoff + a circuit breaker
  (Resilience4j) so transient FIP/AA slowness doesn't cascade; 4xx business errors from Digio
  are not retried.
- All write endpoints accept an `Idempotency-Key` header so retried requests (e.g. from a flaky
  mobile network) don't create duplicate consents or fetch sessions.

## Sandbox testing

- Do not enter real personal account details in the sandbox flow.
- For bank testing, select **FinShareBank OE UAT FIP** in the Digio consent screen and use
  OTP **021069**.
- Sandbox responses are test data, not live AA data — expect fixture-like account/transaction
  values back from `fetchFiData`.

## What's stubbed / needs your confirmation before go-live

- Exact Digio endpoint paths and payload field names in `DigioClientService` and the
  `dto.digio.*` records — these follow the standard AA API shape but should be checked against
  your onboarding reference docs.
- Production auth on the customer-facing endpoints (currently open; see above).
- `redirectUrl` handling assumes Digio returns a hosted consent-approval URL; if instead it
  returns a token for an embedded widget, swap the frontend's `window.location.href` redirect
  in `consent-form.component.ts` for that widget integration.
