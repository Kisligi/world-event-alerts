# Trade-offs and future improvements

Standing rule (Treefa, Prompt 4): whenever we take the faster route because of scope, time, or because this is not a real product, it is recorded here so it can be revisited and improved later.

Each row: what we chose now, what the fuller version would be, why it was deferred, and a rough effort to revisit. "Source" points to the question in [`planning/02-scoping-answers.md`](planning/02-scoping-answers.md) or a later milestone.

| ID | Area | Chosen now (shortcut) | Future improvement | Why deferred | Rough effort | Source |
|----|------|-----------------------|--------------------|--------------|--------------|--------|
| T1 | Architecture | Synchronous in-process matching and dispatch (Option B) | Message broker (RabbitMQ): event exchange, per-channel delivery queues, retries with dead-letter queues (Option C) | Does not fit in one day together with the documentation priority | ~0.5–1 day | Q1 |
| T2 | Event sources | Simulator + manual "publish test event"; real USGS earthquake adapter off by default | Real news and market feeds (need API keys or licensing), GDACS disasters, on by default with health monitoring | No-API-keys rule; offline, repeatable demo | ~2–4 h per source | Q2 |
| T3 | Security | No login; seeded demo users + user switcher; admin area open | Real authentication and authorization (e.g. Spring Security + OIDC), roles, per-user data isolation | Not a production project; short on time | ~0.5 day+ | Q3 |
| T4 | Admin | Monitor and test only (read-only lists, test event, channel toggles) | Admin can edit/disable any user's alerts and manage users | Little value without real login; time | ~2 h | Q4 |
| T5 | Alert rules | Categories + keywords + minimum severity | Region/location filter (structured region on events, region picker in UI) | Needs region data model the brief does not ask for; ~1.5 h | ~1.5 h | Q5 |
| T6 | Delivery | Immediate notifications only | Per-alert daily/hourly digest option | Needs scheduler and digest templates; ~2 h | ~2 h | Q6 |
| T7 | Delivery | Retries run in-process (pending retries are lost if the app restarts) | Durable retry via outbox table or broker dead-letter queue (see T1) | Single-instance prototype; time | ~2–3 h | Q6 |
| T8 | Process | Direct commits to main (no PRs) | Feature branches + pull requests with CI checks before merge | Single developer who reviews and tests before every commit | ~0.5 h setup | Q7 |
| T9 | Data | Demo users are inserted by a Flyway migration, so they would exist in every environment | Seed demo data only in a `demo` profile (e.g. a startup data loader), keep migrations schema-only | Simplest way to have users without sign-up; this is a demo | ~0.5 h | M1 |
| T10 | Validation | Email addresses checked with a simple pattern (one "@", a dot in the domain, no spaces) | Full validation, or confirming the address by sending a verification email | Good enough for a demo; real verification needs user accounts (T3) | ~1–2 h | M1 |
| T11 | API | List endpoints return everything, no pagination | Pageable endpoints (`page`, `size`, `sort`) | Tiny data volumes | ~1 h | M1 |
| T12 | Security | Swagger UI and OpenAPI document are always on | Disable or protect them in production (`springdoc.api-docs.enabled=false`) | Useful for reviewers; no production deployment | ~10 min | M1 |
| T13 | Matching | All enabled rules of the event's category are loaded and checked in Java for every event | Push severity (and maybe keywords) into SQL, or index rules; cache rules in memory | Tens of rules in a demo; clearer, fully tested matching logic | ~2 h | M2 |
| T14 | Matching | Keywords are plain case-insensitive substrings: "crash" also matches "crashes" but also "crashed" or "autocrash"; no phrases, stemming or negation | Word-boundary matching, phrases, stemming, NOT terms, or full-text search (PostgreSQL `tsvector`) | Simple and predictable for a demo | ~2–4 h | M2 |
| T15 | Events | The simulator picks from 12 fixed sample events | Richer generator (random places and numbers), or replaying recorded real events | Enough to exercise every category and severity | ~1 h | M2 |
| T16 | Events | USGS severity comes from magnitude only | Use depth, the USGS PAGER `alert` level, tsunami flag and population exposure | Magnitude is always present and easy to explain | ~1–2 h | M2 |
| T17 | Events | Polling runs inside the app; with two app instances both would poll (the unique constraint prevents double storage, but work is doubled) | A single scheduler (e.g. ShedLock or Quartz cluster mode) or a separate ingestion service | Single-instance prototype | ~1–2 h | M2 |
| T18 | Data | Events are kept forever | Retention policy (e.g. delete events older than 30 days, keep delivery history) | Tiny volumes in a one-day demo | ~1 h | M2 |
