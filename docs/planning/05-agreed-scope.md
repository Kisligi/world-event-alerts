# 05 — Agreed scope and stack (APPROVED 2026-11-20 11:29 UTC)

Date: 2026-11-20. Built from the answers in `02-scoping-answers.md` (Q1–Q8). Shortcuts are tracked in `03-tradeoffs-register.md`. Items marked *(my default)* were not asked as questions; say if you want any changed.

## What we build

A small web application where users create alert rules and get notified by email or Slack when a matching world event arrives, plus an admin view.

**User area (`/`)**
- Pick a seeded demo user with a user switcher (no login, Q3).
- Create, edit, delete, enable/disable alert rules. A rule has: name, one or more categories (`NEWS`, `MARKET`, `DISASTER`), optional keywords (case-insensitive match on title/description), minimum severity (`LOW` < `MEDIUM` < `HIGH` < `CRITICAL`), one or more channel targets (email address, Slack webhook URL) (Q5).
- See own notification history.

**Admin area (`/admin`)** (Q4)
- Read-only lists of users and their alerts, ingested events, and the delivery log (per event, alert and channel: sent / failed, attempts, error, message body).
- Publish a test event by hand.
- Enable/disable a channel globally.

**Events** (Q2)
- `EventSource` interface. Built-in simulator emits sample news, market and disaster events on a fixed interval.
- Optional USGS earthquake adapter (real, keyless), off by default, enabled by one config property.

**Delivery** (Q6)
- Immediate. Dedup on (event, alert, channel). Up to 3 attempts with backoff; final status in the delivery log.
- `NotificationChannel` interface; Email and Slack are two implementations. Adding a channel = one new class (documented with a worked example).

## Stack

| Layer | Choice |
|-------|--------|
| Backend | Java 21, Spring Boot 4.1.3 (Q8), Maven wrapper |
| Persistence | PostgreSQL, Spring Data JPA, Flyway migrations *(my default; MyBatis is an option if you prefer)* |
| API docs | OpenAPI + Swagger UI via springdoc-openapi 3.2.0 (verified built against Boot 4.1.2, see `04-version-check.md`) |
| Email | `JavaMailSender` over SMTP to **Mailpit** (web inbox) |
| Slack | Slack incoming-webhook JSON over HTTP to a **WireMock** container standing in for Slack *(my default)*; the admin delivery log also shows each message body |
| Frontend | React 19 + TypeScript, Vite, React Router; plain hooks for data fetching, no Redux *(my default: the app is small)* |
| Tests | JUnit 5, Mockito, Spring Boot test slices, Testcontainers (PostgreSQL), embedded SMTP/HTTP stubs for channels; Vitest for the frontend |
| Run | `docker compose up --build` starts app, PostgreSQL, Mailpit, WireMock (Q1) |

## Git and process (Q7)

- Public repo `world-event-alerts` in your GitHub account, MIT license.
- One commit per milestone directly on `main`, each only after you approve a change summary and the commit message.
- Milestone = compiles, all tests pass, related docs updated.

## Milestones

| # | Milestone | Content |
|---|-----------|---------|
| M0 | Docs skeleton | README, LICENSE, .gitignore, docs/ (decisions, prompt log, review log, trade-offs, planning materials). No code. |
| M1 | Backend and alert API | Spring Boot project, domain model, Flyway schema, user + alert REST API, tests |
| M2 | Events and matching | `EventSource`, simulator, USGS adapter (off), matching engine, tests |
| M3 | Channels and delivery log | `NotificationChannel`, Email, Slack, dispatcher, dedup, retry, delivery log, tests |
| M4 | User UI | React SPA: user switcher, alert CRUD, notification history, Vitest |
| M5 | Admin UI | Users/alerts, events, delivery log, publish test event, channel toggles |
| M6 | One-command run and final docs | docker compose, architecture doc, screenshots, walkthrough |

## Out of scope (see trade-offs register)

Real authentication, user management, region filters, digests, durable retries / message broker, real news and market feeds.
