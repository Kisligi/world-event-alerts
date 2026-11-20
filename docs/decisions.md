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

## ADR-022 — The delivery log is the de-duplication record (M3)

- **Decision:** One `delivery` row per (event, alert rule, channel type, address), backed by a unique constraint. The dispatcher creates the row before sending; if it already exists (or the insert hits the constraint), nothing is sent. The same row then records status (`PENDING`, `SENT`, `FAILED`, `SKIPPED`), attempts, last error and the exact subject and body.
- **Alternatives:** a separate "sent" table or cache for dedup and a separate log; dedup on (event, user) only.
- **Why:** One table answers both "was this already sent?" and "what happened?", so they can never disagree. Keying on the target (not the user) means a user with two Slack channels gets both, but never the same message twice on one of them (ADR-007).

## ADR-023 — Each dispatch step in its own transaction (M3)

- **Decision:** `DeliveryDispatcher` listens with `@TransactionalEventListener(fallbackExecution = true)`, so it runs after the ingestion transaction commits (or immediately when there is none). Reading the plan, creating a delivery and saving its outcome each run in a new transaction (`REQUIRES_NEW`). Network calls happen outside any transaction.
- **Alternatives:** plain `@EventListener` inside the ingestion transaction; one transaction around the whole dispatch.
- **Why:** An event that rolls back never notifies anyone. No database transaction or connection is held while waiting for SMTP, Slack or retry back-off. `REQUIRES_NEW` is required, not a style choice: after commit, the finished transaction's resources are still bound to the thread, and a plain repository call would join it and silently never commit. Spring's documentation recommends `REQUIRES_NEW` for this; `DeliveryIntegrationTest` confirms the rows are committed ([`dev-journal.md`](dev-journal.md)).
- **Amended (M3 review, [`review-log.md`](review-log.md) R7):** the claim "no connection is held while waiting" was wrong. An after-commit listener runs *before* the finished transaction releases its connection, so every delivery held the ingestion's connection for its whole duration and borrowed a second one for each step. Ten concurrent deliveries (the pool size) blocked each other for 30 s and notifications were lost. `EventIngestionService` now stores and matches in a transaction and publishes `WorldEventMatched` only after that transaction has completed, so a delivery holds at most one connection, only during a database step. `DeliveryConcurrencyIntegrationTest` reproduces the problem (pool of 2, 4 parallel events, slow webhook); it failed before the change and passes after it. The listener and `REQUIRES_NEW` are kept, so the dispatcher stays correct if the announcement is ever published inside a transaction.

## ADR-024 — Synchronous dispatch with in-memory retries (M3)

- **Decision:** Deliveries are sent one after another on the ingesting thread. Each gets up to 3 attempts, waiting 1 s and then 2 s (`alerts.delivery.*`, overridable by environment variables). The waiting is behind a small `Sleeper` interface so unit tests run without real delays.
- **Alternatives:** an `@Async` worker pool; an outbox table polled by a worker; a message broker (Option C).
- **Why:** Simplest correct behaviour that meets ADR-007, and easy to reason about and test. The cost (a failing channel delays the caller by about 3 s) is recorded in [`tradeoffs.md`](tradeoffs.md) T19 together with the fix; restart safety is T7.

## ADR-025 — Delivery history survives rule deletion (M3)

- **Decision:** `delivery.alert_rule_id` is `ON DELETE SET NULL` and the rule's name is copied into `alert_rule_name`. Deliveries are deleted only with their event.
- **Why:** The delivery log is the user's notification history and the admin's audit trail; deleting a rule should not rewrite what was sent. The copied name keeps old entries readable.

## ADR-026 — Global channel switch as data, defaulting to on (M3)

- **Decision:** A `channel_setting` row per channel type, seeded on by `V4`, changed with `PUT /api/admin/channels/{type}`. A missing row counts as enabled. A delivery for a switched-off channel is recorded as `SKIPPED` with the reason, not silently dropped. `GET /api/admin/channels` also reports whether each type has an implementation.
- **Why:** The admin can see that a message *would* have gone out. Defaulting to on means a newly added channel works even before its row is seeded; the `implemented` flag shows a type that exists in the enum but has no channel bean (that delivery is recorded as `FAILED`, with 0 attempts).

## ADR-027 — Outgoing HTTP: HTTP/1.1 with timeouts (M3)

- **Decision:** All `RestClient`s (Slack, USGS) use the JDK `HttpClient` forced to HTTP/1.1, with a 5 s connect timeout and a 10 s read timeout (`HttpClientConfig`).
- **Why:** With defaults, the JDK client tried to upgrade plain-`http` connections to HTTP/2 (h2c) and the WireMock Slack stand-in reset the stream, so every Slack message failed with "Received RST_STREAM: Stream cancelled". Slack webhooks do not need HTTP/2. Timeouts were missing too: a hanging webhook would have blocked delivery indefinitely (T19 makes that worse, not better).
- **Amended (M3 review, R7):** SMTP gets timeouts too: 5 s to connect, 10 s to read and 10 s to write (`spring.mail.properties.mail.smtp.*` in `application.yaml`). Jakarta Mail waits forever by default; a check against a server that accepts connections but never answers was still hanging after 20 s. With delivery on the polling thread, one hung mail server would have stopped every event source. `EmailTimeoutTest` checks both the shipped values and that the property names really take effect.

## ADR-028 — Local stand-ins: Mailpit and WireMock, shared by tests and compose (M3)

- **Decision:** Email goes over SMTP to Mailpit (`axllent/mailpit:v1.34.0`); Slack webhooks go to WireMock (`wiremock/wiremock:3.13.2`). The WireMock stubs live in `infra/wiremock/mappings/` and are used both by `compose.yaml` and by the Testcontainers setup in the tests. `/services/...` returns 200; `/services/fail/...` returns 500 to show retries.
- **Why:** The app talks to them exactly as it would to a real SMTP server and real Slack, with no code switches; only configuration differs. One mapping file means tests and the manual demo behave the same. Image versions are pinned for repeatable runs.

## ADR-029 — At most 50 alert rules per user (M3 review)

- **Decision:** Creating a rule fails with 400 once the user has `alerts.rules.max-per-user` rules (default 50, env `ALERTS_MAX_RULES_PER_USER`).
- **Alternatives:** no limit; stream or page rules during matching; match in SQL.
- **Why:** Every event loads all enabled rules of its category into memory, and delivery plans one message per matched target, so memory per event grows with the total number of rules. Without login (T3) anyone could create rules in a loop. A cap keeps the worst case bounded (users × 50 rules × 10 targets) for the cost of one `COUNT` query. Matching at scale is still [`tradeoffs.md`](tradeoffs.md) T13. Found in the memory review ([`review-log.md`](review-log.md) R7).

## ADR-030 — Frontend versions (M4)

- **Decision:** React 19.3.1, React Router 8.5.2, TypeScript 7.0.2, Vite 8.3.8 with `@vitejs/plugin-react` 6.3.0, Vitest 5.1.1, Testing Library (React 16.3.3, jest-dom 7.0.1), jsdom 31.0.1. Exact versions in `package.json`, locked by `package-lock.json`. Node.js 22.22 or newer for development (the strictest `engines` range among these, jsdom's).
- **How chosen:** the current release of each package was looked up on the npm registry on 2026-11-20 together with its release date and peer dependencies (Vitest 5 supports Vite 8; the React plugin needs Vite 8; Testing Library supports React 19). Day-one releases were avoided, as for Spring Boot (ADR-010): jest-dom 8.0.0 came out the day before, so 7.0.1 is used.
- **Checked rather than assumed:** React Router 8 was released after Claude's training data. Its exports (`BrowserRouter`, `Routes`, `Route`, `NavLink`, `Navigate`, `MemoryRouter`) were read from the installed package's type definitions before use, and the build and tests confirm them. TypeScript 7 (7.0.2, released July 2026) type-checks the whole app, tests included, in strict mode.

## ADR-031 — Frontend structure: plain hooks, one fetch wrapper (M4)

- **Decision:** No state library (as agreed in ADR-011). Server data is loaded per page with a small `useApi(load, key)` hook; the selected demo user lives in a React context. All HTTP goes through `api/client.ts`, which turns problem details into an `ApiError`. Routing with React Router's declarative `<Routes>`.
- **Alternatives:** TanStack Query (caching, retries, polling built in); Redux Toolkit; React Router's data routers (loaders and actions).
- **Why:** Two pages with server-owned data. A 40-line hook covers loading, errors, reload and the one subtle case: an answer for the previous user arriving after a switch must be ignored (tested). TanStack Query would be the first thing to add if the app grew (caching across pages, background refetch).

## ADR-032 — Frontend tests replace only the network (M4)

- **Decision:** Page tests render the whole app (`renderApp`: router, user context, pages) and replace `fetch` with a fake backend declared per test (`fakeApi`). Assertions are about what the user sees (roles and accessible names, as Testing Library recommends) and which requests were sent with which bodies.
- **Alternatives:** mocking the API module per test; a mock-server library (MSW); browser end-to-end tests in the suite.
- **Why:** Tests exercise the real components, hooks, routing and the real API client together, so they break when behaviour breaks, not when code is moved. A fake `fetch` needs no extra dependency. An end-to-end check in a real browser against the real backend was run by hand for M4 (screenshots); automating it is [`tradeoffs.md`](tradeoffs.md) T29.

## ADR-033 — Admin UI: one area under /admin, built from the same pieces (M5)

- **Decision:** The admin view is a section of the same web app at `/admin`, with four tabs: users and alerts (read-only, every user's rules grouped by user), events (recent events plus a "publish a test event" form), delivery log (every user's deliveries, filterable by status) and channels (global on/off). The user switcher is hidden there, because admin pages act on all users. One new backend endpoint, `GET /api/admin/alerts`, lists every rule ordered by user; everything else already existed from M2 and M3. The delivery table is shared with the user's notification page (`DeliveryTable`, with an extra User column for the admin), and the events, delivery log and notification pages refresh every 10 s (`useAutoRefresh`).
- **Alternatives:** a separate admin app; one call per user to `GET /api/users/{id}/alerts` instead of a new endpoint; admin editing of other users' rules.
- **Why:** One app means one build, one dev server and later one container (M6). A single admin endpoint avoids one request per user and keeps the ordering on the server. Editing other users' rules was scoped out (tradeoffs T4); without login the admin area is open to everyone (T3). Sharing the table means the user and the admin see a delivery the same way, and one set of tests covers the details panel.

## ADR-034 — One container image; Spring Boot serves the web app (M6)

- **Decision:** A multi-stage [`Dockerfile`](../Dockerfile) at the repository root: Node.js 22 builds the web app, a JDK stage puts the built files into the backend's `static/` resources and packages the jar, and the final image has only the Java 21 runtime and the jar, runs as a non-root user and sizes its heap from the container limit (`-XX:MaxRAMPercentage=75`, limit 1 GB in compose). Spring Boot serves the web app; `WebAppController` forwards the app's own routes to `index.html`. `compose.yaml` builds and starts it with PostgreSQL, Mailpit and WireMock, so `docker compose up --build` is the one command.
- **Alternatives:** a separate web server container (nginx) for the web app with a reverse proxy to the API; Spring Boot's buildpacks (`spring-boot:build-image`); forwarding every unknown path to `index.html`.
- **Why:** One image and one origin: no CORS, no proxy configuration, the same paths as in development, and one container fewer to explain. A plain Dockerfile shows every step to a reader and needs only Docker; buildpacks hide the steps and would need a separate step for the web app. The route list is explicit so that a mistyped API path stays a 404 problem detail instead of an HTML page, at the cost of updating one line when a page is added (the test lists the routes). Base images are pinned to exact versions (`node:22.23.5-alpine3.24`, `eclipse-temurin:21.0.13_9-jdk-noble` / `-jre-noble`), checked on Docker Hub on 2026-11-20 and available for both amd64 and arm64.

## ADR-035 — Slack addresses work the same inside and outside the container (M6)

- **Decision:** A forwarder container (`alpine/socat:1.8.1.1`) shares the app container's network and passes the app's `localhost:8089` on to WireMock. Slack targets keep the address that works in the browser and in development: `http://localhost:8089/services/...`.
- **Problem:** Inside a container, `localhost` is the container itself. With the app in a container, every Slack target that the README, the web app's hint and the tests use (`http://localhost:8089/...`) failed with a connection error; this was checked by stopping the forwarder.
- **Alternatives:** tell users to enter `http://wiremock:8080/...` in the container setup (two different addresses for the same thing, and the browser cannot open that one); rewrite `localhost` addresses in `SlackChannel` (demo logic in production code); run WireMock inside the app container's network (then `docker compose up -d wiremock` for development would also need the app); host networking (not the same on macOS and Windows).
- **Why:** No code change, one address everywhere, and development keeps working, because the forwarder only starts with the app. Real Slack addresses (`https://hooks.slack.com/...`) are not affected.
