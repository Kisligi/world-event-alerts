# 02 — Scoping answers (one question at a time)

| # | Question | Answer | Time (UTC) |
|---|----------|--------|------------|
| Q1 | Which setup should a reviewer need to run the project with one command? | Docker Compose stack (Option B): app + PostgreSQL + Mailpit + Slack stand-in via `docker compose up`. Accepted as recommended. | 2026-11-20 10:39 |
| Q2 | Where should the world events come from? | Simulator (plus admin "publish test event") as default; optional real USGS earthquake adapter (keyless), off by default. Accepted as recommended. | 2026-11-20 10:53 |
| Q3 | How should users and the admin log in? | No login: seeded demo users + user switcher, admin area open; documented as out of scope. Accepted as recommended. Rationale given: "Since this isn't a production project and we're short on time". | 2026-11-20 10:56 |
| Q4 | What should the admin view let an admin do? | Monitor and test: read-only users/alerts, event list, delivery log (sent/failed per channel), publish test event, toggle channels globally. Accepted as recommended. | 2026-11-20 10:59 |
| Q5 | What should a user be able to filter on in an alert rule? | Categories + optional keywords (title/description) + minimum severity, plus channel targets and on/off. Accepted as recommended. Region/location filter (option C) recorded as a future feature in `03-tradeoffs-register.md`. | 2026-11-20 11:11 |
| Q6 | How should notifications be delivered when an event matches? | Immediate; dedup per (event, alert, channel); up to 3 attempts with backoff; final status in delivery log. Accepted as recommended. | 2026-11-20 11:13 |
| Q7 | How should milestones land on GitHub? | Direct commits to main, one per milestone, each after approval. Public repo `world-event-alerts`, MIT license. Rationale given: single developer who reviews and tests before every commit and push. | 2026-11-20 11:17 |
| Q8 | Which Spring Boot line should we build on (Java 21 either way)? | Spring Boot 4.1.3 (stable line since June 2026, newest patch). Accepted as recommended. See `04-version-check.md`. | 2026-11-20 11:20 |

## Appendix — questions exactly as presented

Questions were asked one at a time as multiple-choice cards. The recommended option is marked ★. Every answer was the recommended option.

**Q1. Which setup should a reviewer need to run the project with one command?**
- ★ Docker Compose stack (Option B): one command (docker compose up) starts the app, PostgreSQL, Mailpit and a Slack stand-in. The reviewer only needs Docker.
- JDK 21 only (Option A): ./mvnw spring-boot:run with an H2 database and test mail and Slack servers built into the app. Simpler, less realistic, about an hour faster.
- Compose plus RabbitMQ (Option C): like B, but with message queues and dead-letter retries. The most robust design, but at risk of not fitting in one day.
- Reason given: realistic, because the app reaches mail and Slack only through config URLs. Docker is a common prerequisite for reviewers and fits Treefa's Postgres and OpenShift experience.

**Q2. Where should the world events come from?**
- ★ Simulator plus optional USGS: a simulator emits sample events and admins can publish one by hand. A real USGS earthquake feed (no key) exists but is off by default.
- Simulator only: all events are simulated or published by hand. Fully offline and repeatable, but no real data at all.
- Simulator plus USGS on by default: real earthquakes appear on startup. More convincing, but the default run needs internet and can't be repeated exactly.
- Reason given: the pluggable source design is proven with one real adapter, while the default run stays offline and repeatable. News and market feeds all need API keys, so those stay simulated.

**Q3. How should users and the admin log in?**
- ★ No login, demo user switcher: seeded demo users and a user switcher in the UI. The admin area is open. Documented as out of scope, not production-ready.
- Basic login, in-memory demo users: Spring Security HTTP Basic with a demo user and a demo admin (published demo passwords, not secrets). Roughly +1 hour.
- Real user accounts: sign-up, stored hashed passwords and roles. Adds half a day or more, which would push other scope out.
- Reason given: the brief doesn't mention security, and with one day the time is better spent on alerts, channels and docs.

**Q4. What should the admin view let an admin do?**
- ★ Monitor and test: read-only list of users and alerts, event list, delivery log (sent/failed per channel), publish a test event, turn channels on or off.
- Monitor only: read-only lists. Smallest scope, but the demo needs another way to trigger an event.
- Monitor, test and manage: everything in the first option, plus editing or disabling any user's alerts and managing users. Roughly +2 hours.
- Reason given: publishing a test event makes the demo instant and testable. The channel toggle shows the channel abstraction at work. User management adds little without real login.

**Q5. What should a user be able to filter on in an alert rule?**
- ★ Category, keywords, severity: categories (news, market, disaster), optional keywords on title or description, and a minimum severity (low to critical). Plus channel targets and an on/off switch.
- Category and severity only: no keyword matching.
- Add region/location filter: needs region data on every event and a region picker. Roughly +1.5 hours.
- Reason given: keywords cover most "only what I care about" cases, including place names, at little cost.

**Q6. How should notifications be delivered when an event matches?**
- ★ Immediate, dedup, 3 retries: sent right away, never twice for the same event, alert and channel. Up to 3 attempts with backoff, final status in the delivery log.
- Immediate, no retry: one attempt only; failures are just logged.
- Immediate plus daily digest option: needs a scheduler and digest templates. Roughly +2 hours.
- Reason given: dedup and simple retries are the minimum for alerts to be trustworthy. Digests go to the trade-offs register.

**Q7. How should milestones land on GitHub?**
- ★ Direct commits to main: one commit per milestone straight to main, each after approval. Public repo world-event-alerts, MIT license.
- Branch and PR per milestone: better review trail on GitHub, but more steps per milestone and slower.
- Reason given: the approval before every commit acts as the review; direct commits keep a clean, linear main where every commit is a working rollback point.

**Q8. Which Spring Boot line should we build on (Java 21 either way)?**
- ★ Spring Boot 4.1.3: stable line out since June 2026, newest patch. Brings the Boot 4 changes (Spring Framework 7, Jackson 3).
- Spring Boot 4.2.0: newest release, published the day before. Little real-world mileage.
- Spring Boot 3.5.16: most familiar APIs. Older line; support status unverified at the time of asking (later verified as ended, see `04-version-check.md`).
