# HelpDesk

A full-stack support ticket system built with React, Spring Boot, and PostgreSQL.

HelpDesk separates authentication and user management from ticket management
using two independent Spring Boot services, each with its own PostgreSQL database.

[Live Demo](https://frontend-production-b347.up.railway.app)

![HelpDesk Dashboard](docs/screenshots/dashboard.png)

## What it does

HelpDesk supports three roles:

- **Users** create and manage their own support tickets.
- **Agents** work on tickets assigned to them.
- **Admins** manage users, assign tickets, and view system-wide statistics.

Tickets follow a fixed lifecycle:

`OPEN → IN_PROGRESS → RESOLVED → CLOSED`

Each ticket also has an SLA based on its priority, with the current SLA state
calculated by the backend.

## Architecture

![HelpDesk Architecture](docs/screenshots/architecture.png)
## Tech stack

| Layer     | Stack                                                                 |
|-----------|------------------------------------------------------------------------|
| Frontend  | React 18, TypeScript (strict), Vite, Tailwind CSS, React Router v6, Axios |
| Backend   | Spring Boot 3.3.4, Java 21, Spring Security (custom JWT filter), Spring Data JPA |
| Database  | PostgreSQL 16 (one instance per service)                              |
| Auth      | JWT (jjwt 0.12.6, HS256)                                              |
| Docs      | springdoc-openapi (Swagger UI on both services)                       |

## Quick start

### Option A — Docker Compose (fastest, whole stack)

```bash
cp .env.example .env
# edit .env and set JWT_SECRET (see comments in the file)
docker compose up --build
```

Then open `http://localhost:5173`. Full details, troubleshooting, and
production notes: **[`DEPLOYMENT.md`](./DEPLOYMENT.md)**.A

### Option B — Run each piece manually (best for active development)

Useful if you want hot reload on the frontend or want to iterate on one
backend service without rebuilding a container each time.

1. **[`backend/user-service/README_RUN.md`](./backend/user-service/README_RUN.md)** — start this first (Postgres + the service itself)
2. **[`backend/ticket-service/README_RUN.md`](./backend/ticket-service/README_RUN.md)** — needs user-service already running
3. **[`frontend/README_RUN.md`](./frontend/README_RUN.md)** — needs both backends already running

## Ports (local dev vs. Docker Compose — unambiguous reference)

| Service       | Docker Compose internal (container-to-container) | Host port (both `docker compose` and standalone) |
|---------------|----------------------------------------------------|----------------------------------------------------|
| user-service  | `user-service:8081`                                | `localhost:8081`                                    |
| ticket-service| `ticket-service:8082`                              | `localhost:8082`                                    |
| frontend      | —                                                    | `localhost:5173`                                    |
| user_db       | `user-db:5432`                                     | `localhost:5432`                                    |
| ticket_db     | `ticket-db:5432`                                   | `localhost:5433` *(not 5432 — avoids colliding with user_db)* |

Both backend services' `DATABASE_URL` fallback defaults (used only when
`DATABASE_URL` isn't set — i.e. running via `mvnw spring-boot:run` outside
Docker) already match the host-port column above. Docker Compose itself
never relies on those fallbacks — it always sets `DATABASE_URL` explicitly
to the internal-hostname column.

## Demo accounts

All seeded accounts use the password `Password123!`:

| Email                | Role  |
|-----------------------|-------|
| admin@helpdesk.dev    | ADMIN |
| agent1@helpdesk.dev   | AGENT |
| agent2@helpdesk.dev   | AGENT |
| user1@helpdesk.dev    | USER  |
| user2@helpdesk.dev    | USER  |
| user3@helpdesk.dev    | USER  |

user-service seeds these on first boot; ticket-service then seeds ~12 demo
tickets against their real UUIDs (it needs user-service reachable to do so).

## What each role can do

| | USER | AGENT | ADMIN |
|---|---|---|---|
| Create tickets | ✅ | – | – |
| View tickets | own only | assigned only | all |
| Comment | own tickets | assigned tickets | all tickets |
| Advance status | resolved → closed, own tickets | assigned tickets | any ticket |
| Assign agents | – | – | ✅ |
| Manage users | – | – | ✅ |
| View statistics | own scope | own scope | all |

The frontend hides actions that don't apply to the current role/ticket
state, but this is UX only — every rule above is enforced again in the
backend service layer, which is the actual authority.

## API documentation

- user-service: `http://localhost:8081/swagger-ui.html`
- ticket-service: `http://localhost:8082/swagger-ui.html`

## Tests

Both backend services have JUnit 5 / Mockito unit tests (service layer,
JWT handling, SLA calculation) — no integration/Testcontainers tests yet.

```bash
cd backend/user-service && ./mvnw test
cd backend/ticket-service && ./mvnw test
```

## Project structure

```
helpdesk/
├── docker-compose.yml
├── .env.example
├── DEPLOYMENT.md
├── summary.md              # detailed running log of everything built
├── PROJECT_STATUS.md
├── backend/
│   ├── user-service/       # auth, users, agents — port 8081
│   └── ticket-service/     # tickets, comments, notifications, SLA — port 8082
└── frontend/                # React app — port 5173 (5173 dev / served via nginx in Docker)
```

## Known constraints (do not change without good reason)

- SLA state is computed on read, not by a background worker.
- Ticket status transitions are strict forward-only
  (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`); `CLOSED` tickets are immutable.
- `user-service` is the only JWT issuer; JWT claim names (`sub`, `role`,
  `email`) must stay compatible between both services.

See [`PROJECT_STATUS.md`](./PROJECT_STATUS.md) for the complete list and
current remaining tasks.
