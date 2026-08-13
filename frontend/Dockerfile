# syntax=docker/dockerfile:1

# ---- Build stage ----------------------------------------------------------
FROM node:20-alpine AS build
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# IMPORTANT: Vite inlines VITE_* variables into the bundle at BUILD time, not
# at container start time. They must be passed as build args (see
# docker-compose.yml `frontend.build.args`), not as runtime `environment:`
# entries — setting them only in `environment:` would have no effect on the
# already-built static files.
#
# These should be the URLs the *browser* will use to reach the backend
# services, i.e. their host-published ports (typically localhost:8081 /
# localhost:8082) — NOT the internal Docker service hostnames, which are
# unreachable from the user's browser.
ARG VITE_USER_SERVICE_URL=http://localhost:8081
ARG VITE_TICKET_SERVICE_URL=http://localhost:8082
ENV VITE_USER_SERVICE_URL=${VITE_USER_SERVICE_URL}
ENV VITE_TICKET_SERVICE_URL=${VITE_TICKET_SERVICE_URL}

RUN npx tsc -b && npx vite build

# ---- Runtime stage ----------------------------------------------------------
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

HEALTHCHECK --interval=10s --timeout=3s --start-period=10s --retries=6 \
    CMD wget -qO- http://localhost:80/health || exit 1
