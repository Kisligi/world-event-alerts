# World Event Alerts

Users set up alert rules and get notified by **email** or **Slack** when something important happens in the world: breaking news, market movements, natural disasters. Channels are pluggable, so more can be added later. An **admin view** shows users, alerts, events and every delivery attempt.

This is a one-day prototype built from a deliberately vague product brief. The priority is a well-documented development process: every decision, prompt, correction and shortcut is recorded in [`docs/`](docs/).

> **Status: complete (M6).** `docker compose up --build` starts the whole demo with Docker as the only prerequisite. Users pick a demo user, manage their alert rules and see their notification history in a React web app. An admin area shows every user's alerts, recent events and the full delivery log, and lets the admin publish a test event and switch a channel off. Simulated events (and optionally real USGS earthquakes) arrive continuously, are matched against the rules, and matching events are sent by email and Slack, with every attempt in the delivery log.
>
> **New here?** Read [`docs/walkthrough.md`](docs/walkthrough.md): a 10-minute tour of the running demo, then of the code.

## The brief

> "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later. We need an admin view too."

That was the only input. How it became a concrete scope is documented in [`docs/planning/`](docs/planning/) and [`docs/decisions.md`](docs/decisions.md).

## What it does

- **Alert rules:** a user picks categories (news, market, disaster), optional keywords and a minimum severity, and where to be notified (email address, Slack webhook URL).
- **Events:** a built-in simulator produces realistic sample events; an admin can publish one by hand. An optional real feed (USGS earthquakes, no API key) can be switched on.
- **Delivery:** matching events are sent immediately, never twice for the same event, alert and channel, with up to 3 attempts.
- **Admin view:** users and alerts, events, delivery log, "publish test event", global channel on/off.
- **Runs with one command, no accounts or API keys:** emails land in a local test inbox (Mailpit); Slack messages go to a local Slack stand-in (WireMock).

Full scope: [`docs/planning/05-agreed-scope.md`](docs/planning/05-agreed-scope.md).

## Repository layout

```
backend/         Spring Boot service (Maven, Java 21)
frontend/        React web app (TypeScript, Vite, npm)
docs/            All documentation, planning materials and screenshots
infra/           Configuration for local stand-ins (WireMock stubs for Slack)
Dockerfile       Builds the web app and the backend into one image
compose.yaml     The whole demo: the app, PostgreSQL, Mailpit and WireMock
```

## Tech stack

Java 21 · Spring Boot 4.1.3 · PostgreSQL + Flyway · React 19 + React Router 8 · TypeScript 7 · Vite 8 · JUnit 5, Mockito, Testcontainers · Vitest 5 + Testing Library · Docker Compose · Mailpit · WireMock

Why these: [`docs/decisions.md`](docs/decisions.md).

## How to run

With **Docker** (Docker Desktop, or Docker Engine with the Compose plugin) as the only prerequisite:

```bash
docker compose up --build
```

The first build downloads the Node.js and Maven dependencies and takes a few minutes; later starts take seconds. The app is ready when the log shows `Started WorldEventAlertsApplication` (or `docker compose ps` shows `app` as `healthy`). Then open:

- **The web app: http://localhost:8080** ([alerts](docs/screenshots/m4-my-alerts.png), [new alert](docs/screenshots/m4-new-alert-form.png), [notifications](docs/screenshots/m4-notifications.png), [phone width](docs/screenshots/m4-phone.png)). Pick a demo user at the top right ("Acting as"); there is no login.
- **The admin area: http://localhost:8080/admin** ([users and alerts](docs/screenshots/m5-admin-users-alerts.png), [events and "publish a test event"](docs/screenshots/m5-admin-events.png), [delivery log](docs/screenshots/m5-admin-delivery-log.png), [channels](docs/screenshots/m5-admin-channels.png)). It is open to everyone in this prototype (no login, [T3](docs/tradeoffs.md)).
- Sent emails (Mailpit inbox): http://localhost:8025 ([screenshot](docs/screenshots/m3-mailpit-email.png))
- Slack messages received by the stand-in (WireMock): http://localhost:8089/__admin/requests
- Swagger UI: http://localhost:8080/swagger-ui.html ([screenshot](docs/screenshots/m3-swagger-ui.png))

The ports 8080, 5432, 1025, 8025 and 8089 must be free. Stop with `Ctrl+C`. `docker compose down` removes the containers and **resets the demo data** (the database has no volume; `docker compose stop` keeps it).

The demo starts with three users and no alerts. A quick path through it: as Alice, create an alert for the category "Disaster" with the keyword `earthquake`, the email target and a Slack target `http://localhost:8089/services/demo`; then in Admin → Events publish "M 7.2 earthquake near Tokyo". The email appears in Mailpit, the Slack message in WireMock, and both in Notifications and in the delivery log. More in [`docs/walkthrough.md`](docs/walkthrough.md).

### Development setup

To work on the code, run the backend and the web app on your machine (JDK 21, Node.js 22.22 or newer) and only the infrastructure in Docker:

```bash
# 1. PostgreSQL, Mailpit and WireMock
docker compose up -d postgres mailpit wiremock

# 2. The backend on port 8080
cd backend && ./mvnw spring-boot:run

# 3. The web app with live reload on http://localhost:5173 (forwards /api calls to port 8080)
cd frontend && npm ci && npm run dev
```

Instead of steps 1 and 2, `cd backend && ./mvnw spring-boot:test-run` starts the backend with its own throwaway PostgreSQL, Mailpit and WireMock (Testcontainers, on random ports; `docker ps` shows them).

### Try the API

The same works with `curl` (both setups use port 8080):

```bash
curl -X POST localhost:8080/api/users/1/alerts -H 'Content-Type: application/json' -d '{
  "name": "Major earthquakes",
  "categories": ["DISASTER"],
  "keywords": ["earthquake"],
  "minSeverity": "HIGH",
  "targets": [
    {"type": "EMAIL", "address": "alice@example.com"},
    {"type": "SLACK", "address": "http://localhost:8089/services/T000/B000/demo"}
  ]
}'

# Publish a test event as the admin; the response lists the alert rules it matched,
# and the email and Slack message are sent right away
curl -X POST localhost:8080/api/admin/events -H 'Content-Type: application/json' -d '{
  "category": "DISASTER", "severity": "CRITICAL", "title": "M 7.2 earthquake near Tokyo"
}'
```

### Configuration

**Event sources** are configured with environment variables (with Docker, add them under `app.environment` in `compose.yaml`):

| Variable | Default | Meaning |
|----------|---------|---------|
| `ALERTS_SIMULATOR_ENABLED` | `true` | Sample news, market and disaster events, labelled `[SIMULATED]` |
| `ALERTS_SIMULATOR_INTERVAL` | `15s` | Time between simulated events |
| `ALERTS_USGS_ENABLED` | `false` | Real earthquakes (magnitude 4.5+, past day) from the public USGS feed; needs internet, no API key |
| `ALERTS_USGS_INTERVAL` | `5m` | Time between USGS polls |

To see retries and a failed delivery, use a Slack target under `/services/fail/`, e.g. `http://localhost:8089/services/fail/demo`: the stand-in answers 500, the app tries 3 times and logs the delivery as `FAILED`. To switch a channel off: `curl -X PUT localhost:8080/api/admin/channels/SLACK -H 'Content-Type: application/json' -d '{"enabled": false}'`.

**Delivery** settings:

| Variable | Default | Meaning |
|----------|---------|---------|
| `MAIL_HOST` / `MAIL_PORT` | `localhost` / `1025` | SMTP server (Mailpit locally) |
| `ALERTS_EMAIL_FROM` | `World Event Alerts <alerts@example.com>` | Sender of alert emails |
| `ALERTS_DELIVERY_MAX_ATTEMPTS` | `3` | Attempts per notification |
| `ALERTS_DELIVERY_INITIAL_BACKOFF` | `1s` | Wait before the 2nd attempt; doubled for each further one |
| `ALERTS_MAX_RULES_PER_USER` | `50` | Upper limit on alert rules per user |

## How to test

```bash
cd backend && ./mvnw clean verify     # backend: 158 tests
cd frontend && npm ci && npm test     # frontend: 56 tests (npm run coverage for a coverage report)
```

The backend runs unit tests, web-layer tests and integration tests. The integration tests start real PostgreSQL, Mailpit and WireMock containers with Testcontainers, so **Docker must be running**. `clean` matters after switching branches: compiled classes left in `target/` from another branch can make tests fail that pass on a clean build. Coverage reports: `backend/target/site/jacoco/index.html` and `frontend/coverage/index.html`.

The frontend tests render the real app in a simulated browser (jsdom) and replace only the network: each test defines what the backend answers and checks what the user sees and which requests were sent. Testing rules: [`docs/process.md`](docs/process.md#testing-standard).

## Milestones

Each milestone was reviewed on its own branch and then fast-forwarded onto `main`; changes requested in review are extra commits. A milestone is done when the code compiles, all tests pass and the related docs are updated.

| # | Milestone | Status |
|---|-----------|--------|
| M0 | Docs skeleton and planning records | ✅ |
| M1 | Backend skeleton and alert API | ✅ |
| M2 | Events and matching | ✅ |
| M3 | Channels and delivery log | ✅ |
| M4 | User UI | ✅ |
| M5 | Admin UI | ✅ |
| M6 | One-command run and final docs | ✅ this commit |

## Documentation

| Document | What it contains |
|----------|------------------|
| [`docs/walkthrough.md`](docs/walkthrough.md) | Start here: a guided tour of the running demo, then of the code |
| [`docs/process.md`](docs/process.md) | How we work: ground rules, milestone definition, approval flow |
| [`docs/decisions.md`](docs/decisions.md) | Architecture decision records: what we chose, the alternatives, and why |
| [`docs/tradeoffs.md`](docs/tradeoffs.md) | Every shortcut taken for time or scope, and how to improve it later |
| [`docs/architecture.md`](docs/architecture.md) | How the system is structured: components, API, data, tests, deployment |
| [`docs/prompt-log.md`](docs/prompt-log.md) | The full prompt history, verbatim |
| [`docs/review-log.md`](docs/review-log.md) | Every correction or change request on Claude's proposals |
| [`docs/dev-journal.md`](docs/dev-journal.md) | Per-milestone notes: what was built, problems met, how it was verified |
| [`docs/planning/`](docs/planning/) | Original planning materials, unedited |

## License

[MIT](LICENSE)
