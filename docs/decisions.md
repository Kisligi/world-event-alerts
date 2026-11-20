# Decisions

Architecture decision records (ADRs). Each one says what was decided, what else was considered, and why. Shortcuts taken for time or scope are also listed in [`tradeoffs.md`](tradeoffs.md). The questions as presented, with all options, are in [`planning/02-scoping-answers.md`](planning/02-scoping-answers.md).

Decided by: Treefa, on Claude's proposals. Date: 2026-11-20.

---

## ADR-001 — Stay on Java / Spring Boot / React

- **Decision:** Java 21 + Spring Boot backend, React + TypeScript frontend.
- **Alternatives:** Node.js/TypeScript end to end; Python/FastAPI.
- **Why:** Neither alternative is significantly better for this problem. Spring Boot already provides mail sending, scheduling, an HTTP client, validation, OpenAPI support and good test slices. It is also Treefa's main stack, so Treefa can review the code in depth, which matters more than any marginal gain.

## ADR-002 — Run the whole stack with Docker Compose

- **Decision:** `docker compose up --build` starts the app, PostgreSQL, Mailpit (local SMTP server with a web inbox) and WireMock (Slack stand-in). Prerequisite: Docker only.
- **Alternatives:** (A) single JDK-only app with H2 and embedded fakes, `./mvnw spring-boot:run`; (C) Compose plus RabbitMQ.
- **Why:** Realistic: the app reaches mail and Slack only through configured URLs, exactly as it would in production. A reviewer only needs Docker. Fits Treefa's PostgreSQL / Docker / OpenShift background. Option C did not fit in one day (see [`tradeoffs.md`](tradeoffs.md) T1).
- **Consequence:** Integration tests use Testcontainers, so running the full test suite also needs Docker.

## ADR-003 — Simulated events, with one optional real source

- **Decision:** An `EventSource` interface. The default source is a simulator that emits sample news, market and disaster events; admins can also publish events by hand. A real USGS earthquake adapter exists but is off by default.
- **Alternatives:** simulator only; USGS on by default; real news and market feeds.
- **Why:** The ground rules forbid API keys, and every news and market API Claude knows of needs one (NewsAPI, Alpha Vantage, Finnhub; high confidence). USGS earthquake feeds are public and keyless (high confidence). Keeping it off by default keeps the demo offline and repeatable. GDACS and GDELT are believed keyless but were not verified (medium confidence), so they are not used.

## ADR-004 — No authentication

- **Decision:** Seeded demo users and a user switcher in the UI; the admin area is open.
- **Alternatives:** HTTP Basic with in-memory demo users (+~1 h); real accounts (+½ day).
- **Why:** "This isn't a production project and we're short on time" (Treefa). The brief does not mention security. Documented as a known gap: [`tradeoffs.md`](tradeoffs.md) T3.

## ADR-005 — Admin view: monitor and test

- **Decision:** Read-only lists of users, alerts, events and the delivery log; publish a test event; enable or disable a channel globally.
- **Alternatives:** monitor only; monitor, test and manage users (+~2 h).
- **Why:** "Publish test event" makes the demo instant and testable. The channel toggle shows the channel abstraction at work. User management adds little without real login.

## ADR-006 — Alert rule model

- **Decision:** A rule has a name, one or more categories (`NEWS`, `MARKET`, `DISASTER`), optional keywords (case-insensitive match on title and description), a minimum severity (`LOW` < `MEDIUM` < `HIGH` < `CRITICAL`), one or more channel targets, and an enabled flag.
- **Alternatives:** category and severity only; plus a region filter (+~1.5 h).
- **Why:** Keywords cover most "only what I care about" cases, including place names, at little cost. Region filtering is a valid future feature ([`tradeoffs.md`](tradeoffs.md) T5).

## ADR-007 — Delivery semantics

- **Decision:** Immediate delivery. Deduplicated on (event, alert, channel). Up to 3 attempts with backoff; final status recorded in the delivery log.
- **Alternatives:** single attempt; optional daily digest (+~2 h).
- **Why:** Deduplication and retries are the minimum for alerts to be trustworthy, and they are cheap. Digests and durable retries are deferred ([`tradeoffs.md`](tradeoffs.md) T6, T7).

## ADR-008 — Extensible channels via a strategy interface

- **Decision:** A `NotificationChannel` interface; Email and Slack are two Spring beans implementing it. The dispatcher discovers all channel beans. Adding a channel (e.g. Teams, SMS) means adding one class and its target type, with no change to matching or dispatch.
- **Why:** This is the brief's "flexible enough that we can add more channels later". It will be demonstrated with a worked example in [`architecture.md`](architecture.md), not just claimed.

## ADR-009 — Git workflow: direct commits to `main`

- **Decision:** One commit per milestone directly on `main`, each after Treefa approves the change summary and commit message. Public repository `world-event-alerts`, MIT license.
- **Alternatives:** a feature branch and pull request per milestone.
- **Why:** "I'm the only developer on this project and I review and test the code before every commit and push" (Treefa). The approval step is the review; `main` stays linear, and every commit is a working rollback point.
- **Amended (M1, [`review-log.md`](review-log.md) R5):** every milestone is first pushed to its own review branch (`m1-review`, `m2-review`, …) for local testing; after approval `main` is fast-forwarded to it. Still no pull requests and no merge commits.

## ADR-010 — Spring Boot 4.1.3

- **Decision:** Spring Boot 4.1.3 on Java 21, with springdoc-openapi 3.2.0 for Swagger UI.
- **Alternatives:** 4.2.0 (released the day before, 2026-11-19); 3.5.16.
- **Why:** 4.1 is the current stable line (since June 2026) without day-one risk. Treefa asked for a double check before approving, which found:
  - springdoc-openapi 3.2.0 is built against Spring Boot 4.1.2, so it is compatible.
  - 3.5.x is not a safer fallback: Spring calls 3.5.16 the "last OSS release of the 3.5.x generation". Third-party sources date its OSS end to 30 June 2026.
  - 4.1.x OSS support runs to 31 July 2027 per third-party sources. Not confirmed on spring.io because the development environment's network policy blocked it.
- Details and sources: [`planning/04-version-check.md`](planning/04-version-check.md).

## ADR-011 — Defaults chosen by Claude (not asked as questions)

Proposed in [`planning/05-agreed-scope.md`](planning/05-agreed-scope.md) and approved together with it:

| Choice | Alternative | Why |
|--------|-------------|-----|
| Spring Data JPA + Flyway | MyBatis | Least code for simple CRUD; Flyway keeps the schema explicit and versioned |
| WireMock container as the Slack stand-in | A custom mini service | Zero custom code; records every request; the admin delivery log shows message bodies anyway |
| No Redux; plain React hooks for data fetching | Redux Toolkit | The app has a few screens with server-owned state; Redux would be ceremony |

## ADR-012 — Repository created by Treefa, not Claude

- **Status:** accepted. Treefa created the empty public repository at 11:33 UTC.
- **Context:** Ground rule 2 says Claude creates the public GitHub repository.
- **Decision:** Treefa creates the empty repository; Claude does all commits and pushes after approval.
- **Why:** The environment Claude runs in cannot create GitHub repositories. Recorded so the deviation is visible.

---

Decisions below were made by Claude during implementation, within the approved scope, and presented to Treefa with the milestone that introduced them.

## ADR-013 — Repository layout and package-by-feature (M1)

- **Decision:** `backend/` holds the Maven project; the React app will live in `frontend/`; `compose.yaml` and `docs/` sit at the root. Java packages are grouped by feature (`user`, `alert`, later `event`, `delivery`, `channel`) with a small `common` package.
- **Alternatives:** single Maven project at the root with the frontend built by `frontend-maven-plugin`; package-by-layer (`controller`, `service`, `repository`).
- **Why:** Two independent toolchains (Maven, npm) stay simple and can be built in separate Docker stages. Package-by-feature keeps each concept in one place, which makes the "add a channel" story easy to follow.

## ADR-014 — Errors as RFC 9457 problem details (M1)

- **Decision:** All API errors are `application/problem+json` (Spring's `ProblemDetail`), with an extra `errors` list for field validation failures.
- **Why:** A standard, self-describing error format supported natively by Spring; the frontend can show `detail` and `errors` without custom parsing.
- **Amended (M2):** `ApiExceptionHandler` now extends Spring's `ResponseEntityExceptionHandler`, so Spring's own errors (malformed JSON, out-of-range query parameters, …) are problem details too. A test showed they were plain JSON before. Switching on `spring.mvc.problemdetails.enabled` was tried first and rejected: Boot's handler then took over body validation errors and dropped our `errors` list.

## ADR-015 — Schema owned by Flyway, verified by Hibernate (M1)

- **Decision:** Flyway migrations create the schema; Hibernate runs with `ddl-auto: validate`. One migration per milestone that changes the schema.
- **Why:** Reviewable SQL in version control; the app fails fast if entities and schema disagree. Demo users are inserted by a migration (`V2`), see [`tradeoffs.md`](tradeoffs.md) T9.

## ADR-016 — Testcontainers for integration tests and local runs (M1)

- **Decision:** Integration tests run against a real PostgreSQL 17 started by Testcontainers (`@ServiceConnection`). The same configuration powers `./mvnw spring-boot:test-run`, which runs the app with a throwaway database and no compose file.
- **Alternatives:** H2 in PostgreSQL mode for tests.
- **Why:** Tests exercise the real database and the real migrations; no dialect surprises. Cost: `./mvnw verify` needs Docker (already a prerequisite, ADR-002).

## ADR-017 — Keywords stored lower-case (M1)

- **Decision:** Keywords are trimmed, lower-cased (`Locale.ROOT`) and de-duplicated on save.
- **Why:** Matching is case-insensitive (ADR-006), so storing them normalized avoids duplicates such as "Japan" and "japan" and keeps the matcher simple.

## ADR-018 — Pull-based event sources with one poller (M2)

- **Decision:** An `EventSource` interface with `name()`, `pollInterval()` and `poll()`. One `EventPoller` schedules every enabled source at its own interval on Spring's task scheduler. Sources are switched on or off by `alerts.sources.*` properties (simulator on, USGS off by default).
- **Alternatives:** each source schedules itself with `@Scheduled`; push-based sources (webhooks, message queue).
- **Why:** The real source (USGS) is a feed you poll, and the simulator fits the same shape. Error handling (a failing source or a bad event never stops the others) lives in one place and is tested once. A new source is one class plus one bean definition.

## ADR-019 — Matching: database pre-filter, rules in Java (M2)

- **Decision:** The database returns enabled rules for the event's category; `AlertMatcher` checks severity and keywords in Java. Keywords match as case-insensitive substrings of title or description.
- **Alternatives:** the whole match in SQL (severity ordering on a string column, `LIKE` per keyword); a search engine.
- **Why:** All matching rules sit in one small, fully unit-tested class. Volumes are tiny ([`tradeoffs.md`](tradeoffs.md) T13, T14).

## ADR-020 — Matching announces, delivery listens (M2)

- **Decision:** Ingestion publishes a `WorldEventMatched(eventId, alertRuleIds)` Spring application event and does not know about channels. M3's dispatcher will listen for it after the transaction commits.
- **Why:** Keeps ingestion and delivery independent and separately testable. It is also the seam where a message broker could later be introduced ([`tradeoffs.md`](tradeoffs.md) T1).

## ADR-021 — USGS adapter details (M2)

- **Decision:** Feed `4.5_day.geojson`, polled every 5 minutes; external id = USGS feature id; severity from magnitude (< 5 LOW, < 6 MEDIUM, < 7 HIGH, ≥ 7 CRITICAL); features without id or magnitude are skipped; `title` used when present, built from magnitude and place otherwise.
- **Why:** The format was checked against the USGS documentation and live data before writing code ([`planning/06-usgs-feed-check.md`](planning/06-usgs-feed-check.md)). `title` is in the live data but not in the documented schema, hence the fallback. The past-day 4.5+ feed gives a useful demo immediately; the feed is updated every minute, so 5 minutes is polite and still timely. The magnitude scale is our own simplification ([`tradeoffs.md`](tradeoffs.md) T16).
