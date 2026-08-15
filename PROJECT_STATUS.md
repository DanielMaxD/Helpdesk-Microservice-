# HelpDesk — Project Status

HelpDesk is a full-stack support ticket management system built with React, Spring Boot, and PostgreSQL.

## Current state

### Backend

Both backend services are implemented and independently deployable:

* `user-service` — authentication, users, roles, and JWT issuance
* `ticket-service` — tickets, comments, notifications, SLA tracking, and assignment

Both services use Spring Boot 3.3.4, Java 21, and Maven.

### Frontend

The frontend is implemented with React 18, TypeScript, Vite, Tailwind CSS, React Router, and Axios.

Implemented functionality includes:

* Login and registration
* Persistent authentication sessions
* Role-based dashboards
* Ticket creation, editing, and deletion
* Ticket search and status filtering
* Ticket assignment
* Ticket comments
* Ticket status lifecycle
* SLA status display
* Notifications
* User management for administrators
* Responsive layout

The frontend communicates with the backend through real API endpoints.

### Docker and deployment

The project includes:

* Dockerfiles for both backend services
* A production frontend image using Nginx
* Docker Compose configuration for the complete local stack
* Health checks and service startup dependencies
* `.env.example` for required environment configuration
* Deployment documentation in `DEPLOYMENT.md`

The project is deployed on Railway for the live demo.

## Testing

Both backend services contain JUnit 5 / Mockito unit tests covering service-layer logic, JWT handling, and SLA calculation.

Integration and end-to-end tests are not currently included.

## Known limitations

* SLA state is calculated when ticket data is requested rather than by a background worker.
* Ticket and user lists do not currently use pagination.
* Database schema management currently relies on JPA `ddl-auto: update` rather than a dedicated migration tool.

## Local development

The complete stack can be started with Docker Compose:

```bash
cp .env.example .env
docker compose up --build
```

For running individual services during development, see:

* `backend/user-service/README_RUN.md`
* `backend/ticket-service/README_RUN.md`
* `frontend/README_RUN.md`

## Project documentation

* `README.md` — project overview, architecture, features, setup, and screenshots
* `DEPLOYMENT.md` — deployment and troubleshooting
* `PROJECT_STATUS.md` — current implementation status
