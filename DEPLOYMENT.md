# Deployment — Docker Compose

This runs the complete HelpDesk stack with Docker Compose:

* PostgreSQL database for `user-service`
* PostgreSQL database for `ticket-service`
* `user-service`
* `ticket-service`
* React frontend served by Nginx

For manual development without Docker, see the `README_RUN.md` files in each service directory.

## Prerequisites

* Docker Desktop or Docker Engine with the Compose plugin
* Ports `5173`, `8081`, `8082`, `5432`, and `5433` available on the host

## 1. Configure environment variables

From the project root:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Open `.env` and set a strong `JWT_SECRET`.

A secret can be generated with:

```bash
openssl rand -base64 48
```

The remaining values in `.env.example` provide local development defaults.

## 2. Build and start the stack

```bash
docker compose up --build
```

Use `-d` to run the containers in the background.

On the first build, Maven downloads the backend dependencies and npm installs the frontend dependencies inside the Docker build.

### Startup order

Docker Compose starts the services in dependency order:

1. `user-db` and `ticket-db` start and report healthy.
2. `user-service` starts after `user-db` is healthy and seeds the demo accounts on first boot.
3. `ticket-service` starts after `ticket-db` and `user-service` are healthy, then seeds demo tickets using the users created by `user-service`.
4. The frontend starts after both backend services are healthy.

## 3. Open the application

Frontend:

```text
http://localhost:5173
```

The seeded demo accounts use the password `Password123!`.

| Email                 | Role  |
| --------------------- | ----- |
| `admin@helpdesk.dev`  | ADMIN |
| `agent1@helpdesk.dev` | AGENT |
| `user1@helpdesk.dev`  | USER  |

Swagger UI:

* `user-service`: `http://localhost:8081/swagger-ui.html`
* `ticket-service`: `http://localhost:8082/swagger-ui.html`

## Useful commands

Follow all service logs:

```bash
docker compose logs -f
```

Follow a specific service:

```bash
docker compose logs -f ticket-service
```

Check container status:

```bash
docker compose ps
```

Stop the stack while keeping database volumes:

```bash
docker compose down
```

Stop the stack and remove database volumes:

```bash
docker compose down -v
```

## Frontend API configuration

The frontend API URLs are embedded into the Vite production build.

`VITE_USER_SERVICE_URL` and `VITE_TICKET_SERVICE_URL` are passed as Docker build arguments through `docker-compose.yml`. Changing them after the frontend image has been built does not change the already-generated JavaScript bundle.

If these values are changed in `.env`, rebuild the frontend image:

```bash
docker compose up --build frontend
```

The URLs must be reachable by the user's browser. For local development they normally point to:

```text
http://localhost:8081
http://localhost:8082
```

They should not use the internal Docker service names because those names are only resolvable between containers.

## Troubleshooting

### Port already in use

If one of ports `5173`, `8081`, `8082`, `5432`, or `5433` is already occupied, stop the conflicting process or change the corresponding host-side port in `docker-compose.yml`.

### `ticket-service` does not become healthy or demo tickets are not seeded

Check the service logs:

```bash
docker compose logs ticket-service
```

`ticket-service` requires `user-service` to be available during startup so it can retrieve the seeded users and create demo tickets.

If the database volumes contain stale or corrupted data, reset the stack:

```bash
docker compose down -v
docker compose up --build
```

### Backend requests return `401`

Both backend services must use the same `JWT_SECRET`. In Docker Compose, both services receive this value from `.env`.

### Frontend API requests fail or CORS errors appear

Check that `FRONTEND_URL` in `.env` matches the URL used to access the frontend.

If the frontend host port has been changed, update `FRONTEND_URL` and restart the backend services:

```bash
docker compose up -d user-service ticket-service
```

### Running a backend locally against Docker PostgreSQL

The Docker Compose database ports are:

| Database    | Host port |
| ----------- | --------: |
| `user-db`   |    `5432` |
| `ticket-db` |    `5433` |

The backend services' local `DATABASE_URL` fallbacks match these host ports.

Run a service manually with:

```bash
./mvnw spring-boot:run
```

On Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

See the corresponding `README_RUN.md` for service-specific instructions.

If the wrong database port is used, PostgreSQL may report:

```text
FATAL: database "ticket_db" does not exist
```

## Production considerations

This Docker Compose configuration is intended primarily for local development and demo deployments.

Before using the application with real production data:

* Use a strong, unique `JWT_SECRET`.
* Serve the frontend and APIs over HTTPS.
* Avoid exposing PostgreSQL ports to the public network.
* Replace `spring.jpa.hibernate.ddl-auto: update` with a dedicated database migration strategy such as Flyway or Liquibase.
* Restrict or protect exposed Actuator management endpoints.
