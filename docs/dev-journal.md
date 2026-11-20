# Development journal

Per-milestone notes: what was built, what came up along the way, and how the result was verified. Times are UTC on 2026-11-20.

---

## M0 — Docs skeleton (committed 12:02, `b170acb`)

- Wrote README, LICENSE, `.gitignore` and the `docs/` structure from the planning materials.
- **Problem:** Claude's environment cannot create GitHub repositories, although ground rule 2 says Claude creates it. Treefa created the empty repository instead ([`decisions.md`](decisions.md) ADR-012).
- **Problem:** the first push was refused because the Claude GitHub App had no access to the new repository. Treefa installed the app on the repository; the push then succeeded.
- **Correction:** Treefa removed the `Claude-Session` link from the commit message ([`review-log.md`](review-log.md) R4).
- **Verified:** every relative Markdown link resolves; no secret-like strings in the tree.

## M1 — Backend skeleton and alert API

First version pushed for review on `m1-review` as `a15f180` (12:19); review round below.

**Built**
- Maven project in `backend/` on Spring Boot 4.1.3 / Java 21, with Maven wrapper (script-only, Maven 3.9.11).
- Entities `AppUser`, `AlertRule` (with `ChannelTarget` value objects), Flyway migrations `V1` (schema) and `V2` (three demo users).
- REST API for users and alert rules, validation, RFC 9457 error responses, OpenAPI + Swagger UI.
- `compose.yaml` with PostgreSQL 17 for local runs; `./mvnw spring-boot:test-run` as a compose-free alternative.

**Things learned about Spring Boot 4 (for a reader coming from Boot 2/3)**
- The web starter is `spring-boot-starter-webmvc`; test slices moved into their own starters (`spring-boot-starter-webmvc-test` provides `@WebMvcTest` in `org.springframework.boot.webmvc.test.autoconfigure`).
- Testcontainers 2.x renamed its artifacts (`testcontainers-postgresql`, `testcontainers-junit-jupiter`) and `PostgreSQLContainer` moved to `org.testcontainers.postgresql` and is no longer generic.
- Spring Boot 4 uses Jackson 3; the tests avoid depending on the `ObjectMapper` API by using JSON text blocks.
- All of the above was checked against the Boot 4.1.3 BOM on Maven Central before writing code, and confirmed by compiling.

**Small issues fixed along the way**
- Mockito printed a JDK warning about dynamically loading a Java agent. Fixed by loading Mockito as an explicit `-javaagent` in Surefire (Mockito's recommended setup).
- Swagger UI showed the generic title "OpenAPI definition v0". Added an `OpenAPI` bean with the real title, version and description.

**Verified**
- `./mvnw verify`: 28 tests, 0 failures (unit, `@WebMvcTest`, and integration against PostgreSQL 17 via Testcontainers).
- Manual smoke test: `docker compose up -d postgres`, ran the packaged jar, then `GET /actuator/health` → `UP`; `GET /api/users` → 3 demo users; `POST` a valid alert → 201; `POST` a Slack target `"nope"` → 400 problem detail; Swagger UI → 200 ([screenshot](screenshots/m1-swagger-ui.png)).

**Review round (12:35, [`review-log.md`](review-log.md) R6)**

Treefa pointed out that `ApiExceptionHandler.handleInvalid` was never exercised. Added JaCoCo, then looked for *behaviour* that no test checked, not just uncovered lines:

| Added test | Behaviour it pins down |
|------------|------------------------|
| `AlertRuleControllerTest.businessRuleViolationFromServiceReturns400ProblemDetail` | a service-level rule violation becomes a 400 `application/problem+json` with the service's message (`handleInvalid`) |
| `AlertRuleControllerTest.rejectsRequestThatBreaksOneConstraint` (6 cases) | each request limit (name length, keyword count, blank keyword, target count, missing type, blank address) is enforced and reported against the right field |
| `AlertRuleControllerTest.malformedJsonReturns400`, `updateValidatesBodyLikeCreate`, `getExistingRuleReturnsIt` | broken JSON is a 400; PUT is validated like POST; timestamps are serialized as ISO-8601 strings |
| `AlertRuleServiceTest` (5 new) | unknown user is a 404 rather than an empty list; update replaces collections instead of merging; missing rule on update saves nothing; delete really deletes |
| `ChannelTargetValidatorTest` (2 new cases) | upper-case `HTTPS://` is accepted; a URL without a host is rejected |
| `AlertApiIntegrationTest` (3 new, 3 extended) | business-rule 400 end to end and nothing stored; users only see their own rules; deleting a rule leaves no orphan rows; `createdAt` stays fixed and `updatedAt` moves on update; POST for an unknown user is a 404; API title |

**Bug found by the new tests:** `createdAt` returned by `POST` had nanosecond precision (`…958703115Z`), but PostgreSQL keeps microseconds, so a later `GET` or `PUT` returned `…958703Z` for the same rule. Clients comparing timestamps would see the value change. Fixed by truncating timestamps to microseconds in `AlertRule`.

**Integration tests are now independent:** each one starts by deleting all alert rules, so test order no longer matters.

**Coverage** (JaCoCo, `./mvnw verify`): 48 tests. Lines 97.1% → **98.9%** (173/175), branches 88.9% → **96.3%** (26/27).

Still uncovered, on purpose:
- `WorldEventAlertsApplication.main` (2 lines): only calls `SpringApplication.run`; it is exercised every time the app is started (smoke test above), and a unit test would only re-test Spring.
- One branch of the `switch` in `ChannelTargetValidator`: a hidden branch the compiler adds for a `null` enum value, which cannot happen because Bean Validation rejects a missing `type` first (tested in `rejectsRequestThatBreaksOneConstraint`).

---

## M2 — Events and matching

**Built**
- `world_event` table (Flyway `V3`) with a unique `(source, external_id)` constraint.
- `EventSource` interface, `SimulatedEventSource` (12-entry catalog), `UsgsEarthquakeSource` (off by default), `EventPoller` and `EventSourceConfig`.
- `EventIngestionService`: store once, match, publish `WorldEventMatched`.
- `AlertMatcher` with all matching rules in one class.
- `GET /api/events` and `POST /api/admin/events` (publish a test event).

**Checked before coding:** the USGS feed format, against the documentation and live data ([`planning/06-usgs-feed-check.md`](planning/06-usgs-feed-check.md)). Finding: `title` exists in live data but is not documented, so the adapter has a fallback. Claude's environment cannot reach earthquake.usgs.gov directly, so the adapter is tested against a fixture rebuilt from two real features, not against the live feed.

**Problems found by tests and fixed**
- **Inconsistent error format.** A new test expected `application/problem+json` for an out-of-range `limit` and for malformed JSON; both came back as plain JSON from Spring's default handler. Fixed by having `ApiExceptionHandler` extend `ResponseEntityExceptionHandler`. The first attempt (`spring.mvc.problemdetails.enabled=true`) made 9 existing tests fail: Boot's handler took over body validation and dropped our `errors` list, so it was reverted. Recorded in ADR-014.
- **A meaningless assertion removed.** A draft integration test asserted that four `int` rule ids were "not null", which can never fail. Replaced by a comment; the exact `contains(quakes, anyDisaster)` check already proves the other four rules are excluded.
- **Timestamp precision** (same issue as in M1): `WorldEvent.from` truncates `receivedAt` and `occurredAt` to microseconds, with a unit test.

**Verified**
- `./mvnw verify`: **102 tests**, 0 failures. Coverage: lines **99.4%** (327/329), branches **98.7%** (78/79).
- Still uncovered, on purpose: `WorldEventAlertsApplication.main` and the compiler-generated `null` branch of the `switch` in `ChannelTargetValidator` (both explained under M1).
- Manual smoke test with the simulator every 3 s: events 1–3 arrived from `SIMULATOR` and matched nothing, as expected (the only rule wanted HIGH+ disasters mentioning "earthquake", and the simulated quake was a LOW M 3.1). Event 4 published through `POST /api/admin/events` ("M 7.2 earthquake near Tokyo", CRITICAL) returned `"matchedAlertRuleIds":[1]`. `GET /api/events?limit=0` returned a 400 problem detail. [Swagger screenshot](screenshots/m2-swagger-ui.png).

---

## M3 — Channels and delivery log

**Built**
- `delivery` and `channel_setting` tables (Flyway `V4`). The unique key on (event, rule, channel type, address) is the de-duplication (ADR-022).
- `NotificationChannel` interface with `EmailChannel` (SMTP) and `SlackChannel` (incoming-webhook JSON). Global channel switches with `GET`/`PUT /api/admin/channels`.
- `DeliveryDispatcher`: listens for `WorldEventMatched` after commit, creates a delivery per target, skips switched-off channels, sends with up to 3 attempts (1 s, 2 s back-off), records the outcome.
- `NotificationFormatter`: one subject and plain-text body per message, ending with the rule that caused it.
- `GET /api/users/{id}/notifications` (user history) and `GET /api/admin/deliveries?status=` (admin log).
- `compose.yaml` now also starts Mailpit and WireMock. The WireMock stubs live in `infra/wiremock/mappings/` and are shared with the tests (ADR-028).
- A worked "add a Teams channel" example in [`architecture.md`](architecture.md#worked-example-adding-a-microsoft-teams-channel).

**Problems found and fixed**
- **Designed around a known Spring pitfall: writes after commit.** The dispatcher runs after the ingestion transaction commits, but that transaction's resources can still be bound to the thread, so plain repository calls would join the finished transaction and their writes would be silently lost. Spring's documentation for after-commit callbacks recommends `REQUIRES_NEW` for this case (Claude's knowledge, not re-checked online today). Every step therefore runs in a new transaction from the first version, which also keeps transactions closed during network calls (ADR-023). `DeliveryIntegrationTest` proves the rows really are committed.
- **Every Slack message failed with "Received RST_STREAM: Stream cancelled".** The JDK HTTP client tried to upgrade plain `http` to HTTP/2 (h2c) and WireMock reset the stream. Fixed by forcing HTTP/1.1 for all `RestClient`s, adding 5 s connect and 10 s read timeouts at the same time; there were none before (ADR-027).
- **An exception without a message would have been logged as "null".** Found by looking at the one uncovered branch (`markFailed` with a null error). Instead of testing the null check, the dispatcher now records the exception type in that case, with a test, and the null check was removed.

**Verified**
- `./mvnw verify`: **138 tests**, 0 failures. Coverage: lines **99.6%** (566/568), branches **99.1%** (114/115). Uncovered on purpose: the same two items as in M1 and M2.
- `DeliveryIntegrationTest` runs against real PostgreSQL, Mailpit and WireMock containers: it reads the email back from Mailpit's API and the Slack request from WireMock's request journal.
- Manual smoke test with `docker compose up -d` and the packaged jar:
  - Alice's rule "Major earthquakes" with an email and a Slack target, and Bob's rule with a Slack target on `/services/fail/demo`.
  - Published "M 7.2 earthquake near Tokyo" (CRITICAL): Alice's email arrived in Mailpit ([screenshot](screenshots/m3-mailpit-email.png)) and WireMock recorded one Slack POST with 200. Bob's webhook got 3 POSTs with 500, and the delivery log shows `FAILED`, 3 attempts, "Slack webhook call failed: 500 Internal Server Error: "simulated Slack outage"".
  - Switched Slack off: the next event was logged as `SKIPPED` ("Channel SLACK is switched off by the admin") for Slack, while email was still sent.
  - A simulated CRITICAL earthquake also reached Alice's inbox on its own, so the simulator → matching → delivery path works without the admin endpoint.
  - `GET /api/users/99/notifications` → 404 problem detail. [Swagger screenshot](screenshots/m3-swagger-ui.png).
- **Observed and recorded as a trade-off:** with the broken webhook, `POST /api/admin/events` took 3.1 s instead of ~60 ms, because the retries run on the request thread (T19).

**Review round (14:07, [`review-log.md`](review-log.md) R7)**

Treefa asked for a 15 s simulator interval and a review for memory leaks and out-of-memory risks, to be discussed before fixing.

*How the review was done:* read every class for state that can grow (static or instance collections, caches, ThreadLocals, listeners), for unbounded queries and for unbounded inputs; then ran the packaged app with metrics enabled and sent 12 admin events at once to a rule whose Slack webhook always fails.

*No leak found:* the app has no static caches and no growing collections. Each event and each delivery step uses its own short transaction, so Hibernate's session never accumulates entities. Event and delivery lists are limited in SQL. The poller schedules with a fixed delay, so slow polls cannot pile up. Heap stayed at about 70 MB.

*Found and fixed (Treefa chose fixes 1–3 on a decision card):*

| # | Finding | Evidence | Fix | Test |
|---|---------|----------|-----|------|
| 1 | **Connection pool starvation.** The after-commit listener ran before the ingestion transaction released its connection, and each delivery step borrowed a second one | 12 parallel events: all 10 connections busy for 30 s, 2 requests failed with 500, only 3 of 10 stored events got a delivery | Publish `WorldEventMatched` after the transaction has completed (ADR-023 amended) | `DeliveryConcurrencyIntegrationTest` (pool of 2, 4 parallel events, 1 s webhook): failed with "Connection is not available" before the fix, passes after it; `EventIngestionServiceTest` checks commit happens before the announcement |
| 2 | **No SMTP timeouts** | A send to a socket that accepts but never answers was still hanging after 20 s | Connect 5 s, read 10 s, write 10 s (ADR-027 amended) | `EmailTimeoutTest`: shipped values are set; with a 0.5 s timeout a send to a silent server fails fast, proving the property names work |
| 3 | **No limit on rules per user**, so memory per event was unbounded | Code reading: matching loads every enabled rule of the category | At most 50 rules per user, configurable (ADR-029) | `AlertRuleServiceTest` (at and just below the limit), `AlertApiIntegrationTest` (51st rule → 400 with the shipped default, other users unaffected) |

*Load check repeated after the fixes:* the same 12 parallel events all returned 201 in about 3.4 s (the retry waits, T19). All 12 deliveries were recorded as `FAILED` after 3 attempts, as expected for the broken webhook. There were no pool errors, and 0 connections were in use while the deliveries waited.

*Recorded, not fixed:* request body size (T25); unbounded table growth and container memory settings for M6 (T26, T18).

**Coverage after the review round:** 145 tests. Lines **99.7%** (573/575), branches **99.1%** (116/117); the same two intentional gaps.

