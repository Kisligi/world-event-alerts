# 03 — Trade-offs and future improvements register

Standing rule (Treefa, 2026-11-20 11:11 UTC): whenever we take the faster route because of scope, time, or the fact that this is not a real product, record it here so it can be revisited later. Will become `docs/tradeoffs.md` (linked from `docs/decisions.md`).

Columns: what we chose now, what the better/fuller version would be, why we deferred it, and a rough effort to revisit.

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
