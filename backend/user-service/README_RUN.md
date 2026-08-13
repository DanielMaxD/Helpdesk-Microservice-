# Running user-service standalone

No local Maven install is required — this project includes the Maven Wrapper
(`mvnw` / `mvnw.cmd`), which downloads the exact pinned Maven version on first
use. Use `./mvnw` (macOS/Linux) or `mvnw.cmd` (Windows) instead of `mvn` if you
don't already have Maven installed.

Timezone note: both services force the JVM default timezone to UTC in code
(`TimeZone.setDefault(...)` in the `main()` method), so timestamps are correct
regardless of your machine's OS timezone (e.g. `Asia/Calcutta`). You do not
need to set `JAVA_TOOL_OPTIONS` or any `-Duser.timezone` flag.

---

## macOS / Linux (bash/zsh)

### 1. Start Postgres for user_db

```bash
docker run --name helpdesk-user-db \
  -e POSTGRES_USER=helpdesk \
  -e POSTGRES_PASSWORD=helpdesk \
  -e POSTGRES_DB=user_db \
  -p 5432:5432 \
  -d postgres:16
```

### 2. Run the service

From `backend/user-service/`:

```bash
chmod +x mvnw          # first time only
./mvnw clean test
./mvnw spring-boot:run
```

(If you already have Maven installed, plain `mvn clean test` / `mvn spring-boot:run` works identically.)

The service starts on `http://localhost:8081` and seeds demo accounts on first boot.

### 3. Verify

```bash
curl -s http://localhost:8081/actuator/health

curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@helpdesk.dev","password":"Password123!"}'
```

---

## Windows (PowerShell)

### 1. Start Postgres for user_db

```powershell
docker run --name helpdesk-user-db `
  -e POSTGRES_USER=helpdesk `
  -e POSTGRES_PASSWORD=helpdesk `
  -e POSTGRES_DB=user_db `
  -p 5432:5432 `
  -d postgres:16
```

### 2. Run the service

From `backend/user-service/`:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

(If you already have Maven installed, `mvn clean test` / `mvn spring-boot:run` works identically. VS Code's Java extension also bundles its own Maven and can run the app via the inline "Run" link above `main()` in `UserServiceApplication.java` without needing either.)

### 3. Verify

PowerShell aliases `curl` to `Invoke-WebRequest`, which has different syntax
from real curl. Use `curl.exe` explicitly (ships with Windows 10+) to use the
same commands as macOS/Linux, or use the native `Invoke-RestMethod` form below.

```powershell
# Using curl.exe (same syntax as macOS/Linux)
curl.exe -s http://localhost:8081/actuator/health

curl.exe -s -X POST http://localhost:8081/api/auth/login `
  -H "Content-Type: application/json" `
  -d '{"email":"admin@helpdesk.dev","password":"Password123!"}'
```

```powershell
# Or the PowerShell-native way
Invoke-RestMethod -Uri http://localhost:8081/actuator/health

Invoke-RestMethod -Uri http://localhost:8081/api/auth/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"email":"admin@helpdesk.dev","password":"Password123!"}'
```

---

Swagger UI (any OS): http://localhost:8081/swagger-ui.html

## Demo accounts (all use password: Password123!)

| Email                 | Role  |
|------------------------|-------|
| admin@helpdesk.dev      | ADMIN |
| agent1@helpdesk.dev      | AGENT |
| agent2@helpdesk.dev      | AGENT |
| user1@helpdesk.dev       | USER  |
| user2@helpdesk.dev       | USER  |
| user3@helpdesk.dev       | USER  |

## If `./mvnw` or `mvnw.cmd` doesn't run on your machine

The wrapper scripts need internet access on first run (they download Maven
itself into `~/.m2/wrapper/dists/`). If your environment blocks that, or the
script itself won't execute for any reason, you have two fallbacks that behave
identically:

1. Use `mvn` directly if you already have Maven 3.9+ installed.
2. Regenerate fresh wrapper files yourself (requires Maven installed once):
   ```bash
   mvn -N wrapper:wrapper -Dmaven=3.9.9
   ```
