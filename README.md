# World Event Alerts

Users set up alert rules and get notified by **email** or **Slack** when something important happens in the world: breaking news, market movements, natural disasters. Channels are pluggable, so more can be added later. An **admin view** shows users, alerts, events and every delivery attempt.

This is a one-day prototype built from a deliberately vague product brief. The priority is a well-documented development process: every decision, prompt, correction and shortcut is recorded in [`docs/`](docs/).

> **Status: M0, planning complete.** No application code yet. Scope and stack are agreed; see [Milestones](#milestones).

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

## Tech stack

Java 21 · Spring Boot 4.1.3 · PostgreSQL + Flyway · React 19 + TypeScript (Vite) · JUnit 5, Mockito, Testcontainers, Vitest · Docker Compose · Mailpit · WireMock

Why these: [`docs/decisions.md`](docs/decisions.md).

## How to run

Not runnable yet. From milestone M6 the whole stack will start with:

```bash
docker compose up --build
```

The only prerequisite will be Docker with Compose.

## Milestones

Each milestone is one commit on `main`. A milestone is done when the code compiles, all tests pass and the related docs are updated.

| # | Milestone | Status |
|---|-----------|--------|
| M0 | Docs skeleton and planning records | ✅ this commit |
| M1 | Backend skeleton and alert API | ⏳ |
| M2 | Events and matching | ⏳ |
| M3 | Channels and delivery log | ⏳ |
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
| [`docs/planning/`](docs/planning/) | Original planning materials, unedited |

## License

[MIT](LICENSE)
