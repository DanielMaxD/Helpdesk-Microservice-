# Running the frontend

The frontend depends on both backend services running first (see their own
`README_RUN.md` files) — user-service on `http://localhost:8081`,
ticket-service on `http://localhost:8082`.

This has been verified in this environment: `npm install`, a full TypeScript
type-check (`npx tsc -b`), and a production build (`npx vite build`) all
complete cleanly with zero errors.

## 1. Configure API URLs (optional)

Defaults already point at `localhost:8081` / `localhost:8082`, so this step
is only needed if you're running the backends on different ports/hosts.

**macOS/Linux/Windows (all the same, it's just a file copy):**
```bash
cp .env.example .env
```
```powershell
Copy-Item .env.example .env
```

## 2. Install and run

**macOS/Linux:**
```bash
cd frontend
npm install
npm run dev
```

**Windows (PowerShell):**
```powershell
cd frontend
npm install
npm run dev
```

Opens on `http://localhost:5173`. Vite's dev server has hot reload, so edits
show up instantly.

## 3. Sign in

Use any of the seeded demo accounts (password `Password123!` for all):

| Email                 | Role  |
|------------------------|-------|
| admin@helpdesk.dev      | ADMIN |
| agent1@helpdesk.dev      | AGENT |
| user1@helpdesk.dev       | USER  |

Or click "Create an account" to register a new USER account against the real
user-service API.

## What's implemented

- Login and registration, both fully wired to user-service
- Persistent session (refreshing the page keeps you signed in via the stored JWT)
- Role-aware app shell (sidebar nav differs for USER/AGENT/ADMIN)
- Dashboard with live ticket statistics from ticket-service, scoped by role
  automatically by the backend (admin sees everything, agents see their
  assigned tickets, users see their own)
- **Ticket list** (`/tickets`) — role-scoped fetch, search by title, status
  filter, SLA fuse per row
- **Ticket detail** (`/tickets/:id`) — status stepper with a real "advance"
  action button gated by role/ownership, inline edit (admin or assigned
  agent), admin assignment picker, comment thread with posting (disabled on
  closed tickets, matching the backend rule), delete (admin)
- **New ticket** (`/tickets/new`) — priority/category pickers with SLA
  hours spelled out
- **Admin users** (`/admin/users`, admin-only route) — change role,
  activate/deactivate, delete; a user's own row has those controls disabled
  to match the backend's self-protection rules
- **Notifications** (`/notifications`) — list with type icons, unread
  highlighting, mark-as-read

All pages call the real backend APIs — nothing is mocked.

## Building for production

```bash
npm run build
```
Outputs static files to `dist/` (type-checks first, then bundles). Verified
working in this environment.
