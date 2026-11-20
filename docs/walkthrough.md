# Walkthrough

A guided tour for someone who has never seen this project. It covers the running demo in about 10 minutes, then shows how one event travels through the code, and finally where the reasoning behind each choice is written down.

## 1. Start it

```bash
docker compose up --build
```

Docker is the only prerequisite; no accounts or API keys. The first build takes a few minutes. When `docker compose ps` shows `app` as `healthy`, open http://localhost:8080.

Keep two more tabs open:
- **Mailpit** (every email the app sends): http://localhost:8025
- **WireMock** (every Slack message the app sends): http://localhost:8089/__admin/requests

What is running ([`architecture.md`](architecture.md#deployment-m6)): the app (Spring Boot, serving the API and the React web app), PostgreSQL, Mailpit as a local mail server, WireMock as a local stand-in for Slack, and a small forwarder that lets the app reach WireMock at the same address as the browser (ADR-035).

## 2. The demo

**a. Be a user.** The app opens as Alice Analyst ("Acting as", top right). There is no login; the three demo users are seeded (ADR-004). Alice has no alerts yet.

**b. Create an alert.** Select **New alert**:
- Name: `Major earthquakes`
- Categories: Disaster (checked by default); Keywords: `earthquake`; Minimum severity: High
- Target 1 is Alice's email, already filled in. **Add target**, choose Slack, address `http://localhost:8089/services/demo`.

Save. The alert appears as a card ([screenshot](screenshots/m4-my-alerts.png)). Try a Slack address like `not-a-url`: the backend rejects it and the form shows why.

**c. Make something happen.** Go to **Admin → Events** ([screenshot](screenshots/m5-admin-events.png)). Publish a test event: category Disaster, severity Critical, title `M 7.2 earthquake near Tokyo`. The answer says it matched 1 alert.

Within a second:
- Mailpit shows an email to alice@example.com with the subject `[CRITICAL] Disaster: M 7.2 earthquake near Tokyo`.
- WireMock shows a POST to `/services/demo` with the Slack JSON `{"text": "*[CRITICAL] Disaster: ...*\n..."}`.
- **Notifications** (as Alice) lists both as Sent; select one to read the exact message.

Meanwhile the simulator publishes a sample event every 15 seconds (labelled `[SIMULATED]`, see Admin → Events). Any that are disasters with "earthquake" in the text and at least High severity notify Alice too.

**d. See a failure.** As Bob Trader, create an alert with only Market checked, minimum severity High, with a Slack target `http://localhost:8089/services/fail/markets`. The stand-in answers 500 for every `/services/fail/` address. Publish a Market / High event. In **Admin → Delivery log** the Slack delivery is **Failed after 3 attempts** (1 s, then 2 s apart), with the error "500 Internal Server Error: simulated Slack outage" when selected. Bob's other targets are not affected ([screenshot](screenshots/m6-compose-delivery-log.png)).

**e. Switch a channel off.** **Admin → Channels**: switch Slack off ([screenshot](screenshots/m5-admin-channels.png)) and publish another earthquake. The email is sent; the Slack delivery is recorded as **Skipped** and never sent. Switch Slack on again.

**f. Look at the rest.** **Admin → Users and alerts** shows every user's rules ([screenshot](screenshots/m5-admin-users-alerts.png)). The delivery log filters by status. The full API is in Swagger UI at http://localhost:8080/swagger-ui.html.

**Optional: real earthquakes.** Add `ALERTS_USGS_ENABLED: "true"` under `app.environment` in `compose.yaml` and restart: the app then polls the public USGS feed (magnitude 4.5+, past day; needs internet, no key) every 5 minutes.

`docker compose down` resets everything (T34).

## 3. One event through the code

Backend code is under `backend/src/main/java/io/github/kisligi/alerts/`, one folder per concept (ADR-013). Read it in the order an event travels:

| Step | Where | What happens | Why this way |
|------|-------|--------------|--------------|
| 1. An event appears | `event/source/SimulatedEventSource`, `UsgsEarthquakeSource`, or `event/EventController` (admin publish) | Each source turns its input into an `IncomingEvent` | Sources are plug-ins behind `EventSource` (ADR-018) |
| 2. Polling | `event/source/EventPoller` | Asks each enabled source for new events at its own interval; a failing source does not stop the others | ADR-018 |
| 3. Storing | `event/EventIngestionService` | Skips an event already stored (same source and id), saves it, finds matching rules, all in one transaction | ADR-020, ADR-023 |
| 4. Matching | `matching/AlertMatcher` | Category, minimum severity and keywords (any keyword, case-insensitive, in title or description); the database pre-filters by category | ADR-006, ADR-019 |
| 5. Announcing | `event/WorldEventMatched` | Published **after** the transaction has committed, so no database connection is held while sending | ADR-020; the M3 review found and fixed pool starvation here (R7) |
| 6. Delivering | `delivery/DeliveryDispatcher` | For each rule target: create a `Delivery` row (its unique key is the de-duplication), skip if the channel is off, send with up to 3 attempts, record the outcome; each step in its own transaction | ADR-007, ADR-022, ADR-023, ADR-024 |
| 7. The message | `delivery/NotificationFormatter` | One subject and plain-text body, ending with the rule that caused it | T20 |
| 8. Channels | `channel/NotificationChannel`, `EmailChannel`, `SlackChannel` | Email over SMTP; Slack as a webhook POST. A new channel is one new class ([worked example](architecture.md#worked-example-adding-a-microsoft-teams-channel)) | ADR-008 |
| 9. Reading it back | `delivery/DeliveryController` | A user's notifications and the admin delivery log | ADR-025 |

Everything else supports this path: `alert/` (rules and their API, validation of targets), `user/` (demo users), `channel/ChannelSettingService` (the global on/off switch), `common/` (shared enums, error responses as problem details, HTTP client timeouts) and `web/` (serving the web app's pages). Configuration is in `backend/src/main/resources/application.yaml`; the schema is in `db/migration/`.

The **web app** is in `frontend/src/`:
- `App.tsx`: the routes and the top bar.
- `api/client.ts`: every call to the backend, turning error responses into readable messages.
- `useApi.ts`: loading data for a page, ignoring answers that arrive after the user has switched.
- `pages/`: My alerts, Notifications, and `pages/admin/` for the four admin tabs.
- `components/`: the alert form and card, the delivery table shared by the user and the admin.

## 4. How it is tested

- **Backend:** 158 tests; `cd backend && ./mvnw clean verify` (needs Docker). Unit tests for the rules, web-layer tests for every endpoint, and integration tests against real PostgreSQL, Mailpit and WireMock containers: for example, an event really arrives in Mailpit and WireMock, the same event twice notifies once, and a broken webhook is tried 3 times ([`architecture.md`](architecture.md#backend-tests)).
- **Frontend:** 56 tests; `cd frontend && npm ci && npm test`. They render the whole app and replace only the network ([`architecture.md`](architecture.md#frontend-tests)).
- **By hand, in a real browser,** at every UI milestone and for the container stack in M6 ([`dev-journal.md`](dev-journal.md)).

The testing rule ([`process.md`](process.md#testing-standard)): tests check behaviour that matters, not lines. What is left uncovered, and why, is listed per milestone in the dev journal.

## 5. Where the reasoning is

| Question | Document |
|----------|----------|
| What did the brief say, and how did it become a scope? | [`planning/`](planning/), especially [`05-agreed-scope.md`](planning/05-agreed-scope.md) |
| Why was X chosen over Y? | [`decisions.md`](decisions.md) (ADR-001 to ADR-035) |
| What was simplified, and what would the real version be? | [`tradeoffs.md`](tradeoffs.md) (T1 to T35) |
| What happened during each milestone, what broke, how was it verified? | [`dev-journal.md`](dev-journal.md) |
| What exactly did the human ask, and when? | [`prompt-log.md`](prompt-log.md) |
| Where did the human correct or redirect the AI? | [`review-log.md`](review-log.md) |
| How did we work? | [`process.md`](process.md) |

The project was built in one day by Treefa with Claude (an AI assistant) writing the code and documents. Treefa set the ground rules, answered the scoping questions, reviewed and tested every milestone locally, and approved every commit and push ([`process.md`](process.md)).
