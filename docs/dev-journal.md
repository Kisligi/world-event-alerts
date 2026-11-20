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
