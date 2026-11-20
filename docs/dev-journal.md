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
