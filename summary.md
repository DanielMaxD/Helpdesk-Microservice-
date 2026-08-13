# HelpDesk Project — Handoff Summary

This document exists so a **new chat** can continue this project with zero
context loss. It was written at the end of a long build spanning multiple
Claude Free usage windows.

## How to use this document

1. Start a new chat and attach **both** this `summary.md` **and** your current
   local project folder (or the last `helpdesk-checkpoint.zip` you were given)
   — a summary alone gives Claude context but not file access. Claude will
   need to re-read or re-receive the actual files to keep editing them, since
   a new chat has no memory of, or access to, files from this one.
2. Tell the new Claude: *"Read summary.md, then continue the project from
   where it left off. Do not rebuild anything already marked complete."*
3. Everything below is accurate as of the end of the previous conversation.
   If you've since run the project and hit errors, paste the exact error
   output in your first message — that's more useful than re-describing the
   symptom.

**Session log:**
- *Earlier session:* both backend services built to completion; frontend
  pages built through Dashboard/Login/Register only.
- *Previous session:* remaining 5 frontend pages built (TicketList,
  TicketDetail, NewTicket, AdminUsers, Notifications) — this is the version
  most of this document describes.
- *Session before this one:* audited the already-built frontend against a
  fresh continuation instruction line-by-line against the real backend
  controllers/DTOs — found the pages themselves already matched the API
  contracts correctly (no invented endpoints, no drift), but found and fixed
  a genuine responsive-design gap (app shell had no mobile handling at all).
  Then built the entire Docker/deployment layer: both backend `Dockerfile`s,
  `frontend/Dockerfile` + `nginx.conf`, root `docker-compose.yml`, root
  `.env.example`, `DEPLOYMENT.md`, and root `README.md`.
- *This session:* fixed a real config bug the person found during their own
  local verification — see "DATABASE_URL port bug — fixed this session"
  below.

---

## Project overview

A microservices helpdesk/support-ticket system, built from an original
detailed specification (`HELPDESK_CLAUDE_FREE_TIER_MAX_OUTPUT.md`) plus
follow-up continuation/audit instruction documents, all treated as source of
truth. Built incrementally across many responses due to Claude Free usage
limits, with explicit checkpoints between sessions — this file is the latest
checkpoint.

**Person's environment:** Windows, VS Code, Docker Desktop, running things
locally by downloading a zip Claude produces via the sandbox/computer tool
and `present_files`. Not using Claude Code — this is the claude.ai chat
interface with the computer/bash/file tools. Person's OS timezone is
`Asia/Calcutta` (this already caused a real bug — see Audit Fixes below).

---

## Architecture decisions — locked in, do not change without a strong reason

- **Backend:** Java 21, Spring Boot **3.3.4**, Maven (+ Maven Wrapper, pinned
  to Maven 3.9.9 — `mvnw`/`mvnw.cmd` included in both services, no local
  Maven install required)
- **Two independent Spring Boot services, two independent Postgres databases**
  (no cross-database foreign keys, no shared DB, no message queue):
  - `user-service` — port `8081`, database `user_db`
  - `ticket-service` — port `8082`, database `ticket_db`
- **Frontend:** React 18 + TypeScript (strict) + Vite + Tailwind CSS +
  React Router v6 + Axios + lucide-react icons — port `5173`
- **Auth:** JWT via `jjwt` 0.12.6, HS256. **user-service is the sole issuer.**
  ticket-service only *validates* tokens (same `JWT_SECRET`, never generates
  its own). Both services' `JwtService` classes must stay claim-compatible
  (`sub` = user id, `role` claim, `email` claim).
- **IDs:** UUIDs everywhere, via Hibernate `@UuidGenerator`
- **Lombok** for entity/DTO boilerplate (`@Getter @Setter @Builder` etc.)
- **Package layout** (both services): `controller / service / repository /
  entity / dto / exception / config / security` (+ `client` in ticket-service
  for the user-service REST client). Constructor injection throughout.
- **Error shape**, identical in both services (`ErrorResponse` record +
  `@RestControllerAdvice GlobalExceptionHandler`):
  ```json
  { "timestamp": "...", "status": 400, "error": "Bad Request", "message": "...", "path": "/api/..." }
  ```
- **Timezone:** `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` as the
  first line of `main()` in both `*Application.java` classes, plus
  `spring.jpa.properties.hibernate.jdbc.time_zone: UTC` in both
  `application.yml`. Permanent fix — no `JAVA_TOOL_OPTIONS` needed.
- Both `@SpringBootApplication` classes exclude
  `UserDetailsServiceAutoConfiguration` (neither service uses Spring
  Security's `UserDetailsService`/`AuthenticationManager` — pure custom JWT
  filter auth) — this removes the "generated security password" warning at
  its source rather than suppressing it.
- Config via env vars with local-dev fallback defaults in `application.yml`
  (e.g. `${JWT_SECRET:local-dev-default}`). **Docker/production must always
  set `JWT_SECRET` explicitly for both services — never rely on the fallback
  outside local dev.**
- Delivery convention established with this person: build in the sandbox
  filesystem, verify what's actually verifiable (see below), zip the whole
  `helpdesk/` project, deliver via `present_files`. This person runs things
  locally in VS Code, not in-chat.

---

## Repository structure

```
helpdesk/
├── PROJECT_STATUS.md              ← previous checkpoint doc (superseded by this file)
├── .gitignore
├── backend/
│   ├── user-service/              ← COMPLETE (34 files)
│   │   ├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
│   │   ├── pom.xml
│   │   ├── README_RUN.md          ← bash + PowerShell instructions
│   │   └── src/main/java/com/helpdesk/userservice/
│   │       ├── config/            (OpenApiConfig, DataSeeder)
│   │       ├── controller/        (AuthController, UserController, AgentController)
│   │       ├── dto/                (RegisterRequest, LoginRequest, AuthResponse, UserResponse, UpdateUserRequest)
│   │       ├── entity/             (User, Role)
│   │       ├── exception/          (GlobalExceptionHandler, ErrorResponse, ResourceNotFoundException, BadRequestException)
│   │       ├── repository/         (UserRepository)
│   │       ├── security/           (JwtService, JwtAuthenticationFilter, SecurityConfig, RestAuthenticationEntryPoint, RestAccessDeniedHandler)
│   │       └── service/            (AuthService, UserService)
│   └── ticket-service/            ← COMPLETE (51 files)
│       ├── mvnw, mvnw.cmd, .mvn/wrapper/maven-wrapper.properties
│       ├── pom.xml
│       ├── README_RUN.md          ← bash + PowerShell instructions
│       └── src/main/java/com/helpdesk/ticketservice/
│           ├── client/             (UserServiceClient — REST calls to user-service)
│           ├── config/             (OpenApiConfig, TicketDataSeeder)
│           ├── controller/         (TicketController, NotificationController)
│           ├── dto/                (Ticket*, Comment*, Notification*, UserDto, SlaState, ...)
│           ├── entity/             (Ticket, TicketComment, Notification, Priority, Status, Category, NotificationType)
│           ├── exception/          (+ ServiceUnavailableException vs user-service)
│           ├── repository/         (TicketRepository, TicketCommentRepository, NotificationRepository)
│           ├── security/           (same pattern as user-service, JwtService is validate-only, + AuthUtils helper)
│           └── service/            (TicketService, NotificationService, SlaCalculator)
├── frontend/                       ← COMPLETE, including responsive shell (39 files)
│   ├── package.json, vite.config.ts, tailwind.config.js, tsconfig*.json
│   ├── README_RUN.md               ← bash + PowerShell instructions
│   ├── Dockerfile, nginx.conf, .dockerignore
│   ├── .env.example
│   └── src/
│       ├── api/                    (client.ts, authApi, userApi, ticketApi, notificationApi)
│       ├── types/                  (auth.ts, ticket.ts — mirror backend DTOs exactly)
│       ├── context/AuthContext.tsx, hooks/useAuth.ts
│       ├── routes/                 (ProtectedRoute, RoleRoute)
│       ├── layouts/AppLayout.tsx   (mobile drawer state added this session)
│       ├── components/             (Sidebar + Topbar now responsive; SlaFuse, PriorityBadge, StatusPill, StatCard, FormField, ErrorBanner, FullScreenSpinner)
│       └── pages/                  (all 9 pages — Login, Register, Dashboard, TicketList, TicketDetail, NewTicket, AdminUsers, Notifications, NotFound)
├── docker-compose.yml               ← NEW this session
├── .env.example                     ← NEW this session (root)
├── DEPLOYMENT.md                    ← NEW this session
└── README.md                        ← NEW this session
```

Both backend services also have `src/test/java/...` with JUnit 5 + Mockito
tests (unit tests only — no integration/Testcontainers tests were built).

---

## user-service — COMPLETE

- `POST /api/auth/register` (public, always creates role `USER`, never trusts
  client-supplied role), `POST /api/auth/login` (public)
- `GET /api/users/{id}` (self or ADMIN), `GET /api/users` (ADMIN only),
  `PUT /api/users/{id}` (ADMIN only — partial update: name/role/active),
  `DELETE /api/users/{id}` (ADMIN only), `GET /api/agents` (ADMIN only, lists
  role=AGENT users)
- Business rules: an ADMIN cannot demote, deactivate, or delete **their own**
  account (self-protection checks in `UserService`)
- BCrypt password hashing, Bean Validation on all request DTOs, Swagger UI at
  `/swagger-ui.html`, Actuator health at `/actuator/health`
- `DataSeeder` (CommandLineRunner, skips if `users` table not empty) creates:

  | Email | Role |
  |---|---|
  | admin@helpdesk.dev | ADMIN |
  | agent1@helpdesk.dev | AGENT |
  | agent2@helpdesk.dev | AGENT |
  | user1@helpdesk.dev | USER |
  | user2@helpdesk.dev | USER |
  | user3@helpdesk.dev | USER |

  All demo accounts share password `Password123!`.

---

## ticket-service — COMPLETE

- Entities: `Ticket` (title, description, priority, status, category,
  createdBy UUID, assignedAgent UUID nullable, createdAt, updatedAt,
  resolvedAt, dueAt), `TicketComment` (ticketId, userId, message, createdAt —
  plain UUID columns, intentionally no JPA relationship across the
  user/ticket boundary), `Notification` (userId, type, message, read,
  createdAt)
- Enums: `Priority` LOW(24h)/MEDIUM(8h)/HIGH(4h)/CRITICAL(1h) — SLA hours are
  a method on the enum itself; `Status` OPEN→IN_PROGRESS→RESOLVED→CLOSED
  (strict forward-only state machine, enforced in `TicketService` via an
  `ALLOWED_TRANSITIONS` map — CLOSED tickets are fully immutable, including
  comments); `Category` ACCOUNT/PAYMENT/TECHNICAL/BILLING/OTHER;
  `NotificationType` TICKET_ASSIGNED/TICKET_UPDATED/TICKET_RESOLVED/
  SLA_WARNING; `SlaState` (dto-only, computed, never persisted)
  ON_TRACK/AT_RISK/BREACHED
- Endpoints: `POST /api/tickets`, `GET /api/tickets/{id}`,
  `GET /api/tickets/my`, `GET /api/tickets/assigned` (AGENT/ADMIN only),
  `GET /api/tickets` (ADMIN only), `PUT /api/tickets/{id}`,
  `PUT /api/tickets/{id}/status`, `PUT /api/tickets/{id}/assign` (ADMIN
  only), `DELETE /api/tickets/{id}` (ADMIN only),
  `POST/GET /api/tickets/{id}/comments`, `GET /api/tickets/statistics`
  (scope auto-adjusts by caller role), `GET /api/notifications`,
  `PUT /api/notifications/{id}/read`
- Role rules (enforced in `TicketService`, not just route matchers): USER —
  own tickets only, can create tickets, can only transition
  RESOLVED→CLOSED on their own ticket; AGENT — only tickets assigned to
  them, any valid transition on those; ADMIN — everything
- SLA is computed **on read only** (`SlaCalculator`), no background worker —
  by design per spec
- **Service-to-service:** `UserServiceClient` (Spring `RestClient`) calls
  user-service's `GET /api/users/{id}` during assignment, **forwarding the
  caller's own bearer token** (not a separate service secret) — verifies the
  target user exists, has role AGENT, and is active before allowing
  assignment
- `TicketDataSeeder`: since ticket-service can't know user-service's
  randomly-generated demo UUIDs ahead of time, it logs into user-service's
  real `/api/auth/login` as the demo admin at boot, reads back real UUIDs via
  `GET /api/users`, and seeds ~12 realistic demo tickets against them
  (including a couple of intentionally pre-breached/at-risk tickets for demo
  purposes). Skips gracefully (logs a warning, doesn't crash) if user-service
  isn't reachable yet.
- Tests include a genuine **cross-service JWT interoperability test**
  (`JwtServiceTest`) that hand-builds a token the way user-service does and
  proves ticket-service accepts it with a matching secret and rejects it with
  a mismatched one.

---

## frontend — COMPLETE

**Design system** (documented rationale: avoided generic AI-design clichés —
no cream+terracotta, no dark+neon):
- Colors: `ink-950 #0B1220` (dark sidebar/shell), `paper-50 #F7F8FA`
  (content background), status colors are functional not decorative —
  `teal` (on-track/resolved), `amber` (at-risk/high priority), `coral`
  (breached/critical), `indigo` (primary interactive accent)
- Type: Space Grotesk (display), IBM Plex Sans (body), JetBrains Mono
  (ticket IDs, timestamps, SLA countdowns)
- **Signature element:** `SlaFuse` component — a small horizontal bar on
  every ticket row/card showing elapsed-vs-total SLA time, colored by the
  backend's actual computed `slaState`
- `StatusStepper` — a real sequence marker (OPEN→IN_PROGRESS→RESOLVED→CLOSED
  is the ticket's actual backend-enforced lifecycle), used on the detail page

**All pages built and wired to real APIs — nothing mocked:**
- `api/client.ts` — two axios instances, JWT auto-attached, auto-logout on
  401, `getErrorMessage()` helper
- `AuthContext`/`useAuth` — login, register, logout, persistent session
- `ProtectedRoute`, `RoleRoute` (UX-only gating — backend is always the real
  authorization boundary)
- `useUserDirectory` hook — resolves user names for ADMIN views only, since
  `GET /api/users` is admin-only in user-service; USER/AGENT views correctly
  fall back to generic labels ("Assigned", "You") rather than a raw UUID,
  since a non-admin genuinely has no API to resolve an arbitrary user's
  identity — this is an intentional backend boundary respected on purpose,
  not a gap to "fix" by loosening user-service's permissions
- **LoginPage, RegisterPage** — auth flows
- **DashboardPage** — live `TicketStatistics`, auto-scoped by role
- **TicketListPage** (`/tickets`) — role-scoped fetch (my/assigned/all),
  client-side search + status filter, SLA fuse per row, admin-only
  creator/assignee name resolution via the directory hook
- **TicketDetailPage** (`/tickets/:id`) — full header (priority/status/
  category), `StatusStepper`, `SlaFuse` detail variant, a single "advance
  status" action button whose visibility/label is derived from
  `NEXT_STATUS`/role/ownership (mirrors the backend's exact transition and
  permission rules), inline edit form (admin or assigned agent, hidden on
  closed tickets), admin agent-assignment picker, comment thread with
  posting (disabled on closed tickets to match the backend), delete
  (admin, with confirm)
- **NewTicketPage** (`/tickets/new`) — priority/category selects with SLA
  hours spelled out in the option labels
- **AdminUsersPage** (`/admin/users`, `RoleRoute allow={["ADMIN"]}`) — table
  with inline role-change select, activate/deactivate toggle, delete; a
  user's own row has all three controls disabled client-side to match
  user-service's self-protection rules (avoids a confusing 400 from the API)
- **NotificationsPage** (`/notifications`) — type icon per notification,
  unread highlighting, mark-as-read

**Responsive shell (added this session):** the app shell had zero mobile
handling — `Sidebar` was a fixed-width `w-60` element always rendered, with
no breakpoint logic anywhere in `Sidebar`/`Topbar`/`AppLayout`, and both data
tables (`TicketListPage`, `AdminUsersPage`) used `overflow-hidden`, which
would visually clip columns rather than let the user scroll to them on a
narrow viewport. Fixed:
- `AppLayout` now owns `isMobileNavOpen` state and passes it to `Sidebar`/`Topbar`
- `Sidebar` is a slide-in drawer below the `lg` breakpoint (fixed position,
  `-translate-x-full` when closed, backdrop overlay, closes on link click or
  backdrop click) and reverts to the original always-visible static sidebar
  at `lg` and above — desktop appearance is pixel-identical to before
- `Topbar` gained a hamburger button (visible only below `lg`) that opens the
  drawer, and the "Log out" label collapses to icon-only below `sm` to save
  space
- `TicketListPage` and `AdminUsersPage` tables: `overflow-hidden` →
  `overflow-x-auto` on the wrapping container with `min-w-[720px]` on the
  `<table>`, so narrow viewports get a horizontal scroll instead of clipped
  content
- Everything else (Login/Register, the ticket detail header, dashboard stat
  grid, forms) was already responsive on inspection — left untouched

**Verified for real in the sandbox**, after all of the above changes:
`npm install`, `npx tsc -b` (strict type-check), and `npx vite build`
(production bundle) all completed with **zero errors**. 39 files under
`src/`.

### Not yet built anywhere in the project
- No integration/E2E tests (unit tests only, both backend services)
- No pagination on ticket/user lists (fine at seed-data scale)
- `docker compose up --build` has not actually been executed anywhere (see
  "Docker/deployment layer" section below) — the person needs to run it once
  locally and report back

---

## DATABASE_URL port bug — fixed this session

**The bug (found by the person running the project locally, not by me):**
`ticket-service`'s `application.yml` had
`${DATABASE_URL:jdbc:postgresql://localhost:5432/ticket_db}` as its local
fallback default. 5432 is user_db's host port, not ticket_db's. Every
`README_RUN.md`/`docker-compose.yml` in this project deliberately exposes
ticket_db on host port **5433** instead (to avoid colliding with user_db on
5432) — but the fallback default that ships in `application.yml` still said
5432. Running `ticket-service` locally (`mvnw spring-boot:run`) without
manually exporting `DATABASE_URL` therefore connected to user_db's Postgres
instance and failed with `FATAL: database "ticket_db" does not exist`,
because that connection landed on the wrong Postgres instance entirely (one
that has no `ticket_db` database, since it's user_db's own container).

**Root cause was purely in the fallback default — Docker Compose was never
affected.** `docker-compose.yml` has always set `DATABASE_URL` explicitly to
`jdbc:postgresql://ticket-db:5432/ticket_db` (the internal Docker hostname +
internal container port, which is always 5432 regardless of the host-side
5433 mapping) directly in the `ticket-service.environment` block — an
explicitly-set env var always wins over a `${VAR:default}` fallback in
Spring, so this bug was invisible to anyone running the full
`docker compose up` stack. It only surfaced for the hybrid workflow the
person was using: Postgres containers from Docker Compose, backend services
run locally via `mvnw`.

**Fix applied:**
- `backend/ticket-service/src/main/resources/application.yml` — fallback
  default changed from `localhost:5432/ticket_db` to
  `localhost:5433/ticket_db`, with a comment explaining exactly why (matches
  every documented local Postgres setup for ticket_db) and explicitly noting
  Docker Compose never falls through to this value.
- `backend/user-service/src/main/resources/application.yml` — **no value
  changed** (5432 was already correct for user_db in every environment);
  added a short comment for symmetry/clarity only.
- `docker-compose.yml` — no functional change (it was already correct);
  added a comment on `ticket-service`'s `DATABASE_URL` explicitly
  cross-referencing why it's 5432 there and not 5433, so a future reader
  doesn't "fix" it into the same bug in reverse.
- `backend/ticket-service/README_RUN.md` — removed the now-unnecessary
  manual `$env:DATABASE_URL = "..."` / `DATABASE_URL=... ` step from both the
  bash/zsh and PowerShell walkthroughs (the fallback now matches exactly
  what those steps were manually working around), kept it documented as an
  optional override, and added a "Port note" callout near the top explaining
  the 5432-vs-5433 split for anyone who hits the same error again.
- Root `.env.example` — added a comment block explaining why `DATABASE_URL`
  is intentionally absent as a configurable variable for both possible
  reasons (Docker Compose hardcodes it internally; local runs use the
  corrected `application.yml` fallback), so the two environments read as
  unambiguous rather than looking like an oversight.
- Root `README.md` — added a "Ports" table listing every service's internal
  Docker hostname/port vs. host port side by side in one place.
- `DEPLOYMENT.md` — added a troubleshooting entry for exactly this symptom
  (`database "ticket_db" does not exist`) covering the hybrid
  local-services-against-Docker-Postgres workflow specifically.

**Verification:**
- Both `application.yml` files re-validated with `python3 -c "import yaml;
  yaml.safe_load(...)"` — parse cleanly, exact expected `datasource.url`
  values confirmed by printing them back out.
- `docker-compose.yml` re-validated the same way after the comment edit —
  `ticket-service.environment.DATABASE_URL` confirmed unchanged at
  `jdbc:postgresql://ticket-db:5432/ticket_db`.
- Confirmed via `grep -rln "@SpringBootTest\|@DataJpaTest\|@AutoConfigureMockMvc"`
  across the whole backend that **no test in either service loads a Spring
  context or touches a real datasource** — all seven test classes are plain
  Mockito unit tests. This config bug could never have been caught by
  `mvnw test` (with or without this fix) — only by actually starting the
  app, exactly how the person found it. Also means this fix cannot break any
  existing test.
- **Could not actually execute `mvnw clean test` / `mvnw spring-boot:run`
  in this sandbox to give a real pass/fail.** Traced exactly why with
  `bash -x ./mvnw --version`: the Maven Wrapper's own bootstrap step tries
  to `curl` the Maven 3.9.9 distribution from `repo.maven.apache.org`, which
  returns a failure (curl exit 22) because that domain isn't in this
  sandbox's network allowlist — this fails before the wrapper ever gets to
  building the project or running a single test, so it's unrelated to
  whether this fix is correct. This is the same sandbox limitation noted
  throughout this document; nothing new. **The person still needs to run
  `.\mvnw.cmd clean test` and `.\mvnw.cmd spring-boot:run` locally to get a
  real result** — see the exact commands provided in-chat.

---

## Docker/deployment layer — COMPLETE (built the session before this one)

- `backend/user-service/Dockerfile`, `backend/ticket-service/Dockerfile` —
  identical multi-stage pattern: build stage uses `eclipse-temurin:21-jdk`
  and the project's own `mvnw` (same Maven version pin as local dev, `-DskipTests`
  since unit tests already run separately in CI/local dev); runtime stage is
  `eclipse-temurin:21-jre` with `curl` installed specifically so the
  container `HEALTHCHECK` can hit `/actuator/health`. Each has a matching
  `.dockerignore` (excludes `target/`, IDE files).
- `frontend/Dockerfile` — build stage is `node:20-alpine` (`npm ci`, `tsc -b`,
  `vite build`); runtime stage is `nginx:1.27-alpine` serving the static
  bundle, with `frontend/nginx.conf` handling React Router's client-side
  routes (`try_files ... /index.html` fallback) plus a `/health` endpoint.
  **Important, documented in both the Dockerfile and `DEPLOYMENT.md`:**
  `VITE_USER_SERVICE_URL`/`VITE_TICKET_SERVICE_URL` are Vite build-time
  values inlined into the static JS — they're passed as Docker build `args`,
  not runtime `environment:`, and changing them requires
  `docker compose up --build frontend`, not just a restart.
- Root `docker-compose.yml` — 5 services (`user-db`, `ticket-db`,
  `user-service`, `ticket-service`, `frontend`) on one bridge network
  (`helpdesk-net`), 2 named volumes for Postgres data. Postgres containers
  use `pg_isready` healthchecks; both Java services use `curl`-based
  `actuator/health` healthchecks; `ticket-service` and `frontend` both use
  `condition: service_healthy` (not just `service_started`) on their
  dependencies specifically so a first `docker compose up` reliably seeds
  demo tickets without the person needing to manually restart anything.
  `JWT_SECRET` has no fallback in the compose file itself
  (`${JWT_SECRET:?...}`) — forces the person to set a real value in `.env`
  rather than silently inheriting the source's well-known local-dev default.
- Root `.env.example` — every var docker-compose.yml reads, heavily
  commented, especially the build-time-vs-runtime distinction for the
  `VITE_*` vars.
- `DEPLOYMENT.md` — prerequisites, step-by-step first run, what happens in
  what order and why (health-gated startup), useful `docker compose`
  commands, a dedicated section on the Vite build-arg gotcha, a
  troubleshooting section (port conflicts, seeding failures, JWT mismatch
  symptoms, CORS), and a "notes for real production" section (HTTPS, secrets,
  not exposing Postgres ports, `ddl-auto: update` isn't a migration strategy,
  actuator exposure).
- Root `README.md` — project overview, ASCII architecture diagram, tech
  stack table, both quick-start paths (Docker vs. the three manual
  `README_RUN.md` files), demo accounts, a role-permission matrix, Swagger
  links, test commands, project structure tree, and a condensed "known
  constraints" list linking to this file and `PROJECT_STATUS.md` for detail.

**Verification status — read carefully:** `docker-compose.yml` was validated
with `python3 -c "import yaml; yaml.safe_load(...)"` (parses cleanly, all 5
services/2 volumes/1 network present) since there's no `docker compose
config` available in this sandbox. **No Docker daemon or registry access
exists in this sandbox**, so none of the following have actually been
executed anywhere: `docker build` for any of the three images, `docker
compose up`, or the full stack actually starting end-to-end. The Dockerfiles
mirror commands already verified working outside Docker (the same `mvnw`
invocations from each `README_RUN.md`; the same `npm ci`/`tsc -b`/`vite
build` sequence just verified above), which gives reasonable confidence, but
**this genuinely has not been run** — the person needs to run
`docker compose up --build` locally once and report back anything that
breaks (most likely failure mode if something's wrong: a typo in a Dockerfile
path, or a healthcheck timing out if their machine is slow to pull the base
images the first time — `start_period` is set to 45s for the Java services,
which may need lengthening on a slow connection).

---

## Audit fixes already applied — do not reintroduce these bugs

1. Real bug hit by the person: OS timezone `Asia/Calcutta` caused timestamp
   issues. Fixed permanently via `TimeZone.setDefault(UTC)` in both
   `main()` methods (see Architecture section). If asked to touch timestamp
   handling again, preserve this.
2. Maven Wrapper added to both backend services (see Repository structure).
   Verified: wrapper file executable bits survive zip packaging, and a fresh
   extraction + `npm install`/`tsc`/`vite build` all still passed.
3. Both services' `application.yml` `jwt.secret` default values must stay
   **identical strings** between the two services for local/dev convenience
   — if you ever change one, change both, or local runs will silently fail
   JWT validation.
4. `UserDetailsServiceAutoConfiguration` is deliberately excluded in both
   `*Application.java` — don't "fix" the missing `UserDetailsService` bean,
   that's the intended state.

---

## What Claude could and couldn't verify in the sandbox

- **Frontend**: real `npm install`, `tsc -b`, `vite build` were actually
  executed, both in the session that built the pages and again in this
  session after the responsive-layout changes — Node/npm are available in
  the sandbox and npmjs.org is an allowed network domain. Zero errors both
  times.
- **Backend**: `mvn` could **not** be executed — no network route to Maven
  Central in this sandbox. Confirmed precisely *why* this session via
  `bash -x ./mvnw --version`: the wrapper's own bootstrap `curl` call to
  `repo.maven.apache.org` fails (exit 22) because that domain isn't in the
  sandbox's allowlist — this happens before the project's own dependencies
  or tests are ever reached, so it's a pure environment limitation, not a
  project issue. All backend verification was static (package/directory
  consistency, brace balancing, line-by-line comparison of every
  controller/DTO against the exact frontend API calls that use them, and
  this session, YAML-level validation of both `application.yml` files after
  the `DATABASE_URL` fix) plus careful manual code review. **The person
  still needs to be the one who runs `mvn clean test` /
  `mvn spring-boot:run` and reports back any real compiler or runtime
  errors** — don't assume backend correctness beyond what's stated here.
- **Docker**: no Docker daemon or registry access in this sandbox — see the
  "Docker/deployment layer" section above for exactly what was and wasn't
  possible to verify.

---

## Immediate next step

All planned work for the Docker/deployment phase is done, plus the
`DATABASE_URL` port bug the person found is fixed. **The two concrete
actions needed from the person before anything else:**
1. Run `.\mvnw.cmd clean test` and `.\mvnw.cmd spring-boot:run` for
   ticket-service locally (exact commands given in-chat) to confirm the fix
   actually resolves the `database "ticket_db" does not exist` error —
   this could not be executed in the sandbox (see above).
2. Run `docker compose up --build` locally (after `cp .env.example .env` and
   setting a real `JWT_SECRET`) and report back the exact output if
   anything fails — still the single biggest unverified surface in the
   whole project.

After both are confirmed working, natural next candidates, roughly in order
of value for a recruiter-facing demo:
1. Pagination for ticket/user lists (fine at current seed-data scale, but a
   real gap at any realistic scale)
2. Integration/E2E tests (currently unit tests only on both backend services)
3. Polish pass: loading skeletons instead of full-screen spinners, toast
   notifications instead of inline error banners for transient actions
4. A real migration tool (Flyway/Liquibase) in place of `ddl-auto: update`,
   if this is ever going to hold real data

Continue using the established workflow: build directly in the sandbox
filesystem, verify whatever is actually verifiable there (don't claim
untested things work), zip the full `helpdesk/` project, and deliver via
`present_files`. Use maximum available output per response and leave a
precise checkpoint (matching this document's level of detail) if a response
ends before a phase is fully done.
