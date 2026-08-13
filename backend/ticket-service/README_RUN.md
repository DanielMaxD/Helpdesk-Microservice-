# Running ticket-service standalone

ticket-service depends on user-service being reachable (for agent-assignment
verification and, on first boot, for seeding demo tickets against real user
UUIDs). Start user-service first.

No local Maven install is required — this project includes the Maven Wrapper
(`mvnw` / `mvnw.cmd`). Use `./mvnw` (macOS/Linux) or `mvnw.cmd` (Windows)
instead of `mvn` if you don't already have Maven installed.

Timezone note: both services force the JVM default timezone to UTC in code, so
timestamps are correct regardless of your machine's OS timezone — no
`JAVA_TOOL_OPTIONS` workaround needed.

**Port note (read this if you hit `database "ticket_db" does not exist`):**
ticket_db's Postgres uses host port **5433**, not 5432 — user-service's
Postgres already occupies 5432 locally, so ticket_db is deliberately mapped
to a different host port to avoid a collision. This only matters for the
*host* side; inside Docker Compose, `ticket-db` is always reached internally
on port 5432 (its container-internal port), since containers on the same
Docker network don't go through the host port mapping at all. If you're
running ticket-service locally (this page) against Postgres containers
started by `docker compose up` at the repo root instead of the standalone
`docker run` in step 1 below, the same `localhost:5433` applies — the port
mapping is identical either way.

---

## macOS / Linux (bash/zsh)

### 1. Start Postgres for ticket_db

```bash
docker run --name helpdesk-ticket-db \
  -e POSTGRES_USER=helpdesk \
  -e POSTGRES_PASSWORD=helpdesk \
  -e POSTGRES_DB=ticket_db \
  -p 5433:5432 \
  -d postgres:16
```

Host port `5433` is used because user-service's Postgres already occupies `5432`.

### 2. Make sure user-service is already running

From `backend/user-service/`: `./mvnw spring-boot:run` (see its own
`README_RUN.md`). It must be up on `http://localhost:8081` with its demo
accounts seeded *before* you start ticket-service.

### 3. Run ticket-service

From `backend/ticket-service/`:

```bash
chmod +x mvnw          # first time only
./mvnw clean test
./mvnw spring-boot:run
```

No `DATABASE_URL` needs to be set for this — `application.yml`'s local
fallback default already points at `localhost:5433/ticket_db`, matching the
`-p 5433:5432` port mapping used in step 1 above. Only set `DATABASE_URL`
explicitly if your local Postgres is running somewhere else (a different
port, a remote host, etc.):

```bash
DATABASE_URL=jdbc:postgresql://localhost:5433/ticket_db ./mvnw spring-boot:run
```

The service starts on `http://localhost:8082`. On first boot it logs into
user-service as the demo admin, fetches the real UUIDs of the demo accounts,
and seeds ~12 demo tickets referencing them. If user-service isn't reachable
yet, ticket-service still starts — it just logs a warning and skips ticket
seeding (restart it once user-service is up to seed the demo tickets).

### 4. Verify

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@helpdesk.dev","password":"Password123!"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")

curl -s http://localhost:8082/api/tickets/my -H "Authorization: Bearer $TOKEN"
```

---

## Windows (PowerShell)

### 1. Start Postgres for ticket_db

```powershell
docker run --name helpdesk-ticket-db `
  -e POSTGRES_USER=helpdesk `
  -e POSTGRES_PASSWORD=helpdesk `
  -e POSTGRES_DB=ticket_db `
  -p 5433:5432 `
  -d postgres:16
```

### 2. Make sure user-service is already running

From `backend/user-service/`: `.\mvnw.cmd spring-boot:run` in a separate
PowerShell window, and confirm it's serving on `http://localhost:8081`.

### 3. Run ticket-service

From `backend/ticket-service/`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

No `DATABASE_URL` needs to be set for this — `application.yml`'s local
fallback default already points at `localhost:5433/ticket_db`, matching the
`-p 5433:5432` port mapping used in step 1 above. Only set `DATABASE_URL`
explicitly if your local Postgres is running somewhere else (a different
port, a remote host, etc.), e.g.:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5433/ticket_db"
.\mvnw.cmd spring-boot:run
```

Note: `$env:DATABASE_URL = "..."` only applies to the current PowerShell
session/window — if you use it, set it again in any new terminal. If you're
using VS Code's Spring Boot Dashboard or a Run/Debug configuration instead of
the terminal, add environment variables there (the inline "Run" link above
`main()` does not apply custom environment variables).

### 4. Verify

```powershell
$body = '{"email":"user1@helpdesk.dev","password":"Password123!"}'
$login = Invoke-RestMethod -Uri http://localhost:8081/api/auth/login -Method Post -ContentType "application/json" -Body $body
$token = $login.token

Invoke-RestMethod -Uri http://localhost:8082/api/tickets/my -Headers @{ Authorization = "Bearer $token" }
```

---

Swagger UI (any OS): http://localhost:8082/swagger-ui.html (use the
"Authorize" button with a token obtained from user-service's
`/api/auth/login`).

## Important: JWT_SECRET must match

ticket-service validates the exact same JWT that user-service issues. Both
services must be configured with the identical `JWT_SECRET` value. Locally,
both fall back to the same development default if you don't set it — but in
Docker/production you must set `JWT_SECRET` explicitly for both services via
environment variables, never relying on the development fallback.

## If `.\mvnw.cmd` or `./mvnw` doesn't run on your machine

Same fallbacks as user-service: use `mvn` directly if already installed, or
run `mvn -N wrapper:wrapper -Dmaven=3.9.9` once (requires Maven) to regenerate
fresh wrapper files.
