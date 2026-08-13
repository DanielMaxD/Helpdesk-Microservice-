# HelpDesk — Project Status

This zip is a **checkpoint**, not necessarily a project with zero remaining
work, built incrementally across multiple Claude responses. For full
architecture/convention detail and a session-by-session log, see
`summary.md` in this same zip — it's the authoritative handoff doc.

## What's inside this zip (COMPLETE and working)

### `backend/user-service/` — complete, audit-fixed
### `backend/ticket-service/` — complete, audit-fixed

Both: Spring Boot 3.3.4 / Java 21, Maven Wrapper included, UTC timezone
forced in code, Spring Security generated-password warning removed at its
source. See `summary.md` for full endpoint/entity detail.

### `frontend/` — complete, including responsive layout

React 18 + TypeScript (strict) + Vite + Tailwind + React Router v6 + Axios.
**Actually verified in this environment**: `npm install`, `npx tsc -b`, and
`npx vite build` all completed with zero errors, after every change in this
zip including the latest responsive-layout fix.

All pages built and wired to real backend APIs — nothing mocked. Every
endpoint the frontend calls was cross-checked directly against the real
controller/DTO source this session; no invented endpoints or response
shapes:
- Login, Register (persistent session across refresh)
- Dashboard (live, role-scoped ticket statistics)
- Ticket list (search, status filter, SLA fuse per row)
- Ticket detail (status-advance action, real status stepper, inline edit,
  admin assignment picker, comment thread, delete)
- New ticket form
- Admin user directory (role change, activate/deactivate, delete — with
  self-protection matching the backend)
- Notifications (unread highlighting, mark-as-read)
- **Responsive app shell** — sidebar becomes a slide-in drawer with a
  hamburger toggle below the `lg` breakpoint (was previously fixed-width and
  always visible with no mobile handling at all); both data tables scroll
  horizontally instead of clipping on narrow viewports

Design system: ink-navy/paper palette, Space Grotesk/IBM Plex Sans/JetBrains
Mono type system, and the signature "SLA fuse" element used consistently
across list rows and the ticket detail page.

### Docker / deployment layer — NEW this session

- `backend/user-service/Dockerfile`, `backend/ticket-service/Dockerfile` —
  multi-stage builds (Maven Wrapper → JRE runtime with a real
  `actuator/health` container healthcheck)
- `frontend/Dockerfile` + `frontend/nginx.conf` — Vite production build
  served by nginx, with SPA route fallback
- Root `docker-compose.yml` — wires all 5 containers (2× Postgres, both
  backend services, frontend) with health-gated startup ordering so demo
  data seeds reliably on the very first `docker compose up`
- Root `.env.example` — every variable the compose file reads, heavily
  commented
- `DEPLOYMENT.md` — full walkthrough, the Vite build-arg gotcha explained,
  troubleshooting, and production-hardening notes
- Root `README.md` — ties the whole project together (architecture diagram,
  both quick-start paths, role matrix, demo accounts)

**Not yet run:** `docker-compose.yml` was YAML-validated
(`python3 -c "import yaml; yaml.safe_load(...)"`) but a real
`docker compose up --build` has not been executed anywhere — no Docker
daemon in this sandbox. This is the single most important thing to verify
next. See `DEPLOYMENT.md` → Troubleshooting if anything doesn't come up
cleanly.

### Config bug fixed: ticket-service local `DATABASE_URL` fallback

Found by the person running this locally: `ticket-service`'s
`application.yml` fallback default pointed at `localhost:5432/ticket_db`,
but ticket_db is documented (and Docker Compose–mapped) to host port
**5433**, not 5432 — 5432 is user_db's port. Running ticket-service via
`mvnw spring-boot:run` without manually setting `DATABASE_URL` connected to
the wrong Postgres instance and failed with
`FATAL: database "ticket_db" does not exist`.

Fixed: the fallback default now reads `localhost:5433/ticket_db`, matching
every documented local setup exactly. `docker-compose.yml` was **never
affected** — it already sets `DATABASE_URL` explicitly to the internal
Docker hostname (`ticket-db:5432`), which always overrides this fallback.
`user-service`'s config was already correct (5432) and is unchanged.
Full detail, including exactly why `mvnw test` couldn't have caught this
(no test touches the datasource) and what could/couldn't be verified in this
sandbox: `summary.md` → "DATABASE_URL port bug — fixed this session".

## Still not in this zip

- No integration/E2E tests (unit tests only, both backend services)
- No pagination on ticket/user lists (fine at seed-data scale, would matter
  at real scale)
- No real DB migration tool (`ddl-auto: update` is fine for a demo, not for
  production data)

## Run everything right now

**Option A — Docker Compose (whole stack, one command):**
```bash
cp .env.example .env   # then set JWT_SECRET in .env
docker compose up --build
```
Details, troubleshooting: `DEPLOYMENT.md`.

**Option B — run each piece manually (hot reload, active development):**
1. `backend/user-service/README_RUN.md`
2. `backend/ticket-service/README_RUN.md`
3. `frontend/README_RUN.md`
