# Deployment (Docker Compose)

This runs the entire stack — both Postgres databases, both backend
services, and the frontend — with a single command. This is the fastest way
to see the whole app running end to end.

If you'd rather run each piece manually (e.g. for active backend
development with hot reload), use the three `README_RUN.md` files instead —
see the root `README.md` for links. The two approaches are independent; you
don't need Docker to develop locally.

**Verification status:** `docker-compose.yml` and both Dockerfiles have been
validated for syntax (YAML parses cleanly; `Dockerfile` steps mirror the
exact commands already verified working in each `README_RUN.md`), but a full
`docker compose up --build` has **not** been executed in this environment
(no Docker daemon / registry access here). Please run it once locally and
let me know if anything needs adjusting.

## Prerequisites

- Docker Desktop (or Docker Engine + Compose plugin) installed and running
- Ports `5173`, `8081`, `8082`, `5432`, `5433` free on your machine

## 1. Configure environment variables

From the project root:

```bash
cp .env.example .env
```
```powershell
Copy-Item .env.example .env
```

Open `.env` and set a real `JWT_SECRET` — this one has no fallback in
`docker-compose.yml` on purpose, so the stack refuses to start until you've
made a conscious choice instead of silently reusing the well-known
"local development only" default baked into the source. Generate one with:

```bash
openssl rand -base64 48
```

Everything else in `.env.example` already has a working local-dev default.

## 2. Build and start everything

```bash
docker compose up --build
```

Add `-d` to run in the background. First run will take a few minutes —
Maven needs to download dependencies for both Java services and npm needs to
install frontend packages, all inside the build.

What happens, in order:
1. `user-db` and `ticket-db` start and report healthy (`pg_isready`)
2. `user-service` starts once `user-db` is healthy, then seeds 6 demo
   accounts on first boot
3. `ticket-service` waits for **both** `ticket-db` and `user-service` to be
   healthy — it logs into user-service at boot to fetch real user UUIDs and
   seed ~12 demo tickets against them, so it needs user-service to actually
   be answering requests first, not just started
4. `frontend` waits for both backend services to be healthy, then starts

## 3. Open the app

```
http://localhost:5173
```

Sign in with any seeded demo account (password `Password123!` for all):

| Email               | Role  |
|---------------------|-------|
| admin@helpdesk.dev  | ADMIN |
| agent1@helpdesk.dev | AGENT |
| user1@helpdesk.dev  | USER  |

Swagger UI for each backend: `http://localhost:8081/swagger-ui.html` and
`http://localhost:8082/swagger-ui.html`.

## Useful commands

```bash
docker compose logs -f                  # tail all logs
docker compose logs -f ticket-service   # tail just one service
docker compose ps                       # see health status of everything
docker compose down                     # stop everything, keep DB data
docker compose down -v                  # stop everything AND wipe DB volumes
```

## Important: the frontend's API URLs are baked in at build time

Vite inlines `VITE_USER_SERVICE_URL` / `VITE_TICKET_SERVICE_URL` into the
static JS bundle when the image is **built**, not when the container starts.
They're passed as Docker build `args` (see `docker-compose.yml` →
`frontend.build.args` and `.env.example`), not as runtime environment
variables, because setting them only at runtime would have no effect on
already-built static files.

If you change `VITE_USER_SERVICE_URL` or `VITE_TICKET_SERVICE_URL` in `.env`,
a plain restart won't pick it up — you need to rebuild that image:

```bash
docker compose up --build frontend
```

These two values should be whatever your **browser** will use to reach the
backends (typically `http://localhost:8081` / `http://localhost:8082`), not
the internal Docker service names (`user-service` / `ticket-service`) that
the containers use to talk to each other — the browser can't resolve those.

## Troubleshooting

**Port already in use.** Something else on your machine is using `5173`,
`8081`, `8082`, `5432`, or `5433`. Either stop that process or change the
host-side port in `docker-compose.yml` (the left number in `"host:container"`).

**`ticket-service` never becomes healthy / demo tickets aren't seeded.**
Check its logs: `docker compose logs ticket-service`. It needs `user-service`
to be answering `/api/auth/login` at boot to fetch real UUIDs — if
`user-service`'s healthcheck is passing but login is somehow still failing
(e.g. corrupted `user-db` volume from a previous run), try
`docker compose down -v` for a completely clean start.

**Logging in works but every ticket-service call returns 401.** `JWT_SECRET`
differs between `user-service` and `ticket-service`. In this compose setup
both read the same `.env` value, so this would only happen if you edited one
service's environment block directly — keep them identical.

**Frontend loads but API calls fail / CORS errors in the browser console.**
Check that `FRONTEND_URL` in `.env` matches the URL you're actually loading
the frontend from. If you changed the frontend's host port from `5173`,
update `FRONTEND_URL` to match and restart the backend services
(`docker compose up -d user-service ticket-service`).

**Running a backend service locally (outside Docker) against the Postgres
containers started by `docker compose up`.** This works, but mind the host
ports: `user-db` is published on `localhost:5432`, `ticket-db` on
`localhost:5433` (see the `ports:` mapping for each in `docker-compose.yml`).
`user-service`'s local fallback already defaults to 5432; `ticket-service`'s
already defaults to 5433 — matching this compose file exactly — so
`./mvnw spring-boot:run` / `.\mvnw.cmd spring-boot:run` for either service
should work with no `DATABASE_URL` needed, as long as you didn't change
those port mappings. Full detail in each service's own `README_RUN.md`.
Symptom of getting this wrong: `FATAL: database "ticket_db" does not exist`
(ticket-service connected to user_db's Postgres on 5432 instead of ticket_db's
on 5433, or vice versa).

## Notes for a real (non-demo) production deployment

This compose setup is tuned for a local or demo deployment. Before using it
for anything real, at minimum:

- Set a strong, unique `JWT_SECRET` (never the source's dev fallback)
- Put the frontend behind HTTPS (a reverse proxy like Caddy/Traefik/nginx in
  front of this stack, or a CDN) — browsers should never send credentials
  over plain HTTP outside local dev
- Don't publish the Postgres ports (`5432`/`5433`) to the host — they're only
  exposed here for local debugging convenience
- `spring.jpa.hibernate.ddl-auto: update` (used by both services) is
  convenient for demos but isn't a real migration strategy — introducing
  Flyway or Liquibase is the natural next step before this touches
  production data
- Restrict `management.endpoints.web.exposure` further or put actuator
  behind auth if it's reachable from outside your network
