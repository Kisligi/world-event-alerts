# World Event Alerts

Users set up alert rules and get notified by **email** or **Slack** when something important happens in the world: breaking news, market movements, natural disasters. Channels are pluggable, so more can be added later. An **admin view** shows users, alerts, events and every delivery attempt.

This is a one-day prototype built from a deliberately vague product brief. The priority is a well-documented development process: every decision, prompt, correction and shortcut is recorded in [`docs/`](docs/).

> **Status: M3, channels and delivery log.** Users manage alert rules through a REST API; simulated events (and optionally real USGS earthquakes) arrive continuously, are matched against the rules, and matching events are sent by email and Slack, with every attempt in a delivery log. The UI (M4, M5) comes next; see [Milestones](#milestones).

## The brief

> "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later. We need an admin view too."

That was the only input. How it became a concrete scope is documented in [`docs/planning/`](docs/planning/) and [`docs/decisions.md`](docs/decisions.md).

## What we are building

- **Alert rules:** a user picks categories (news, market, disaster), optional keywords and a minimum severity, and where to be notified (email address, Slack webhook URL).
- **Events:** a built-in simulator produces realistic sample events; an admin can publish one by hand. An optional real feed (USGS earthquakes, no API key) can be switched on.
- **Delivery:** matching events are sent immediately, never twice for the same event, alert and channel, with up to 3 attempts.
- **Admin view:** users and alerts, events, delivery log, "publish test event", global channel on/off.
- **Runs with one command, no accounts or API keys:** emails land in a local test inbox (Mailpit); Slack messages go to a local Slack stand-in (WireMock).

Full scope: [`docs/planning/05-agreed-scope.md`](docs/planning/05-agreed-scope.md).

## Repository layout

```
backend/         Spring Boot service (Maven, Java 21)
docs/            All documentation, planning materials and screenshots
infra/           Configuration for local stand-ins (WireMock stubs for Slack)
compose.yaml     Local infrastructure: PostgreSQL, Mailpit, WireMock (the app joins in M6)
```

## Tech stack

Java 21 · Spring Boot 4.1.3 · PostgreSQL + Flyway · React 19 + TypeScript (Vite) · JUnit 5, Mockito, Testcontainers, Vitest · Docker Compose · Mailpit · WireMock

Why these: [`docs/decisions.md`](docs/decisions.md).

## How to run

**Final goal (M6):** `docker compose up --build`, with Docker as the only prerequisite.

**Now (M3)**, with Docker and JDK 21:

```bash
# Option 1: PostgreSQL, Mailpit and WireMock from docker compose, app from Maven
docker compose up -d
cd backend && ./mvnw spring-boot:run

# Option 2: no compose; the app starts its own throwaway PostgreSQL, Mailpit and WireMock
# with Testcontainers (on random ports; `docker ps` shows them)
cd backend && ./mvnw spring-boot:test-run
```

Then open:
- Swagger UI: http://localhost:8080/swagger-ui.html ([screenshot](docs/screenshots/m3-swagger-ui.png))
- Demo users: http://localhost:8080/api/users
- Latest events (a simulated one arrives every 30 seconds): http://localhost:8080/api/events
- Sent emails (Mailpit inbox): http://localhost:8025 ([screenshot](docs/screenshots/m3-mailpit-email.png))
- Slack messages received by the stand-in (WireMock): http://localhost:8089/__admin/requests
- Delivery log: http://localhost:8080/api/admin/deliveries

Try it:

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

**Event sources** are configured with environment variables:

| Variable | Default | Meaning |
|----------|---------|---------|
| `ALERTS_SIMULATOR_ENABLED` | `true` | Sample news, market and disaster events, labelled `[SIMULATED]` |
| `ALERTS_SIMULATOR_INTERVAL` | `30s` | Time between simulated events |
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

## How to test

```bash
cd backend && ./mvnw verify
```

Runs unit tests, web-layer tests and integration tests (138 tests). The integration tests start real PostgreSQL, Mailpit and WireMock containers with Testcontainers, so **Docker must be running**. A coverage report is written to `backend/target/site/jacoco/index.html`. Testing rules: [`docs/process.md`](docs/process.md#testing-standard).

## Milestones

Each milestone is one commit on `main`. A milestone is done when the code compiles, all tests pass and the related docs are updated.

| # | Milestone | Status |
|---|-----------|--------|
| M0 | Docs skeleton and planning records | ✅ |
| M1 | Backend skeleton and alert API | ✅ |
| M2 | Events and matching | ✅ |
| M3 | Channels and delivery log | ✅ this commit |
| M4 | User UI | ⏳ |
| M5 | Admin UI | ⏳ |
| M6 | One-command run and final docs | ⏳ |

## Documentation

| Document | What it contains |
|----------|------------------|
| [`docs/process.md`](docs/process.md) | How we work: ground rules, milestone definition, approval flow |
| [`docs/decisions.md`](docs/decisions.md) | Architecture decision records: what we chose, the alternatives, and why |
| [`docs/tradeoffs.md`](docs/tradeoffs.md) | Every shortcut taken for time or scope, and how to improve it later |
| [`docs/architecture.md`](docs/architecture.md) | How the system is (or will be) structured |
| [`docs/prompt-log.md`](docs/prompt-log.md) | The full prompt history, verbatim |
| [`docs/review-log.md`](docs/review-log.md) | Every correction or change request on Claude's proposals |
| [`docs/dev-journal.md`](docs/dev-journal.md) | Per-milestone notes: what was built, problems met, how it was verified |
| [`docs/planning/`](docs/planning/) | Original planning materials, unedited |

## License

[MIT](LICENSE)
