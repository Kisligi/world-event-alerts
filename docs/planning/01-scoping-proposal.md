# 01 — Scoping proposal (awaiting approval)

Date: 2026-11-20. Status: proposed, not yet approved. Answer to Prompt 1.

## 1. Clarifying questions

Each question has a proposed default, so "defaults OK" is a valid answer.

| # | Question | Proposed default |
|---|----------|------------------|
| Q1 | Where do events come from? Real public feeds or simulated? | A pluggable `EventSource` interface. Default source is a **simulator** that emits realistic sample events (news, market, disaster) every N seconds, plus an admin "publish test event" button. One optional real adapter (USGS earthquakes, keyless), **off by default** so the one-command run works offline and deterministically. |
| Q2 | What does an alert rule look like? | Name, one or more categories (`NEWS`, `MARKET`, `DISASTER`), optional keywords (match title/description, case-insensitive), minimum severity (`LOW` < `MEDIUM` < `HIGH` < `CRITICAL`), one or more channel targets (email address, Slack webhook URL), enabled flag. No free-form query language, no geo filtering. |
| Q3 | Do we need real authentication? | No real login. A few seeded demo users and a "current user" switcher in the UI; the admin view is a separate route. Clearly documented as out of scope / not production-ready. (Alternative: Spring Security HTTP Basic with in-memory demo users, about +1 hour.) |
| Q4 | What should the admin be able to do? | Read-only overview of users and their alerts; list of ingested events; delivery log (per alert, per channel: sent / failed + error); publish a test event; enable or disable a channel globally. No user management (create/delete users). |
| Q5 | Delivery semantics? | Immediate (no digests). Each (event, alert, channel) is delivered at most once (dedup by unique key). Simple retry (e.g. 3 attempts with backoff) and the final status is recorded. No quiet hours, no rate limiting. |
| Q6 | How do we demo email and Slack without accounts? | Email: real SMTP sent to a **local mail catcher** with a web inbox. Slack: real HTTP POST in Slack incoming-webhook format, sent to a **local Slack-webhook stub** that shows received messages. Pasting a real Slack webhook URL would also work, but we would not test against real Slack. |
| Q7 | What may "a single command" assume is installed? | **Docker (with Compose)** only: `docker compose up --build`. Alternative: JDK 21 only: `./mvnw spring-boot:run`. This choice is the main difference between options A and B below. |
| Q8 | Are user UI and admin UI one app? | Yes, one React SPA with two areas (`/` and `/admin`), served by the backend. |
| Q9 | Git workflow? | Direct commits to `main` at each milestone, each after your approval (no PRs). Tell me if you prefer feature branches + PRs. |
| Q10 | Repository name and license? | `world-event-alerts`, MIT license. |
| Q11 | Java / Spring Boot versions? | Java 21. Spring Boot: latest stable GA at scaffold time. I will check and state the exact version before scaffolding instead of guessing now. |

## 2. Assumptions

- This is a one-day prototype for demonstration and evaluation, not production. Single instance, no horizontal scaling, no HA.
- "Something important happens in the world" = an `Event` with category, title, description, severity, source, timestamp, optional tags/region.
- "Flexible enough to add channels" = a `NotificationChannel` strategy interface; each channel is one Spring bean. Adding e.g. Teams or SMS = one new class plus its target type, no change to matching or dispatch. This is demonstrated and documented, not just claimed.
- Matching is done in-process; volume is small (tens of events, tens of alerts).
- No i18n, no mobile layout work beyond "usable", no accessibility audit.
- Tests: JUnit 5 + Mockito + Spring Boot test slices for the backend; Vitest for the frontend. Integration test for channels against an embedded SMTP server and a stubbed HTTP endpoint.
- Documentation and prompt log are written along the way, not at the end.
- All demo config (ports, demo users, mail catcher host) is non-secret and committed; nothing secret is needed at all.

## 3. Real data sources — what I know and what I don't

| Source | Category | Key needed? | Confidence |
|--------|----------|-------------|------------|
| USGS earthquake GeoJSON feeds | Disaster | No | High: well-known, public, keyless. |
| GDACS (UN/EC disaster alerts) RSS/feeds | Disaster | I believe no | Medium: I believe it is public and keyless, but I have not verified the current format. |
| GDELT | News | I believe no | Medium: believed keyless, not verified; large and noisy. |
| NewsAPI.org | News | Yes (free tier with key) | High. Violates the no-keys rule. |
| Alpha Vantage, Finnhub, etc. | Market | Yes (free key) | High. Violates the no-keys rule. |
| Unofficial Yahoo Finance endpoints | Market | No | Unofficial and unstable; I would not rely on them. |
| Publishers' RSS feeds | News | Usually no | Exist, but terms of use vary; unsure about each. |

Conclusion: news and market events are simulated; disaster events can optionally be real via USGS. Real feeds are never required to run.

## 4. Options

All three share the same core scope:

- User area: create / edit / delete / enable alert rules; see own notification history.
- Event ingestion via `EventSource` (simulator + admin "publish test event").
- Matching engine, dispatcher, Email + Slack channels behind `NotificationChannel`, delivery log.
- Admin area as described in Q4.
- REST API documented with OpenAPI / Swagger UI.
- docs/: README, decisions (ADR-style), architecture, prompt log, review log, planning materials.

### Option A — "Lean monolith, JDK only"

Spring Boot (Java 21) + H2 + Flyway, React + TypeScript (Vite) built by `frontend-maven-plugin` and served from the jar. Email goes to an embedded SMTP catcher (GreenMail) and Slack to a built-in stub endpoint, both active only in a `demo` profile. Run: `./mvnw spring-boot:run` (needs JDK 21; the wrapper downloads Maven and Node).

- Pros: fewest moving parts, fastest to build, nothing but a JDK needed, simplest for you to validate.
- Cons: fakes live inside the app process (less realistic, must be cleanly profile-isolated); H2 instead of a real database; reviewer still needs JDK 21 installed.

### Option B — "Compose stack" (recommended)

Same backend and frontend as A, but `docker compose up --build` starts:
- the app (multi-stage Docker build: Node builds the SPA, Maven builds the jar),
- PostgreSQL + Flyway migrations,
- Mailpit (local SMTP catcher with web inbox, e.g. http://localhost:8025),
- a Slack-webhook stub (WireMock container, or a tiny page inside the app that lists received payloads).

Tests: unit + slice tests; Testcontainers for Postgres integration tests.

- Pros: realistic (the app speaks real SMTP and real HTTP to external-looking services, configured purely by URLs); a reviewer only needs Docker, which is the most common prerequisite; matches your PostgreSQL / Docker / OpenShift background; visible proof of delivery in the Mailpit inbox.
- Cons: requires Docker; first build is slower; a bit more config to keep working; Testcontainers tests need Docker locally too.

### Option C — "Event-driven with a broker"

Option B plus RabbitMQ: events → exchange → matcher → per-channel delivery queues with retry and dead-letter queues; optional real USGS + GDACS adapters on by default.

- Pros: most "architecturally honest" for extensible channels and retries; showcases your messaging experience.
- Cons: does not fit in one day together with the documentation priority; more failure modes; harder to keep every main commit green. I would only mention it in docs as the "next step".

### Why I am not proposing another stack

I considered Node/TypeScript end-to-end and Python/FastAPI. Neither is significantly better here: Spring Boot already gives us mail (`JavaMailSender`), scheduling, `RestClient`, validation, OpenAPI and good test slices, and you can review it in depth. React + TypeScript + Vitest is also your stack. Recommendation: stay on Java 21 / Spring Boot / React + TypeScript.

## 5. Recommendation

**Option B**, with the optional USGS adapter off by default. If you would rather not require Docker, Option A is a close second and saves about an hour.

## 6. Proposed milestones (each = one approved commit on main)

1. M0 — Repo skeleton: README, docs/ (decisions, prompt log, review log, planning materials), .gitignore, license. No code.
2. M1 — Backend skeleton: Spring Boot project, domain model, Flyway schema, alert CRUD REST API + tests.
3. M2 — Events and matching: `EventSource`, simulator, matching engine + tests.
4. M3 — Channels: `NotificationChannel`, Email + Slack, dispatcher, delivery log, retry + tests.
5. M4 — User UI: React SPA for alert management and notification history + Vitest tests.
6. M5 — Admin UI: overview, events, delivery log, publish test event, channel toggles.
7. M6 — One-command run (compose), architecture doc, screenshots, final README and walkthrough.
