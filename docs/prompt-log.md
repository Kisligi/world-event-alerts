# Prompt log

The full prompt history of this project, in order. Typed prompts are verbatim. Answers given by tapping an option on a multiple-choice question are listed as "choice" entries; the full question and all options are in [`planning/02-scoping-answers.md`](planning/02-scoping-answers.md).

Times are UTC on 2026-11-20. "Response" summarizes what Claude did; the resulting files are linked.

---

## Prompt 1 — Treefa, 2026-11-20 10:24 UTC

> A product manager handed me the following short brief (I have no other information):
>
> "We want users to be able to set up alerts so they get notified when something important happens in the world — like breaking news, market movements, natural disasters, that kind of thing. Should work for both email and Slack. Make it flexible enough that we can add more channels later. We need an admin view too."
>
> My goal is to get from this vague description to a working implementation. The task must be completed within one day, so let's keep the scope narrow. The priority is a well-documented development process.
>
> Ground rules:
> 1. Commit and push at each major milestone with a meaningful commit message. A milestone is done when the code compiles, all tests pass, and the related documentation is updated.
> 2. You will create the public GitHub repository in my account and perform all commits and pushes yourself, but only after asking for and receiving my explicit permission each time. Before asking, show me a summary of the changes and the proposed commit message. Never create, commit, push, force-push, rewrite history, or change repository settings without my approval.
> 3. Every commit on the main branch must leave the project in a working state, so we can always roll back to a previous good commit if something breaks.
> 4. Store documentation in a dedicated docs/ folder (e.g. docs/decisions.md, docs/prompt-log.md, docs/architecture.md, docs/review-log.md). Keep README.md in the project root.
> 5. Document the full prompt history used during the process as part of the project. Also keep docs/review-log.md up to date: whenever I reject, correct, or request changes to your proposal, record what you proposed, what I changed, and why.
> 6. Include all materials produced during planning and development: design documents, decision logs, prompt drafts, intermediate results, notes, sketches, screenshots, etc.
> 7. By the end, an outside reader must clearly understand what we built, why, and how, and how the finished code works. They should be able to run the project with a single command, without any API keys or accounts.
> 8. Never commit or publish sensitive data such as secrets, keys, or environment variables.
> 9. If you are unsure about something (e.g. whether an API exists), say so. Do not make things up.
> 10. Language: All project artifacts must be in English, including source code, comments, commit messages, documentation, and our conversation. If I occasionally write in Hungarian, always respond in English.
>
> The brief does not specify anything, so we need to agree on the exact scope and the technologies to use.
>
> My skill set:
> Backend: Java (8–21), Spring Framework, Spring Boot, Quartz, J2EE (JMS, JMX), JBoss, Camunda BPM
> Integration: Microservices, REST, Swagger / OpenAPI, JMS, IBM MQ, RabbitMQ, SWIFT (MT, MX), JSON, XML, YAML
> Databases: Oracle, PostgreSQL, MSSQL, SQL, MyBatis
> DevOps & CI/CD: OpenShift (Kubernetes), Jenkins, Argo CD, Maven, Git, Bitbucket, Docker (basic)
> Testing: JUnit, Mockito, JUnit Jupiter, Vitest, Postman, SoapUI (Groovy), JMeter, SonarQube
> Frontend: React.js, Redux.js, JavaScript, TypeScript, HTML, CSS / SCSS
>
> I listed these only as options; none of them are mandatory. I prefer building on my own stack so I can meaningfully validate the code, but if something is significantly better, argue for it. We will decide on the technologies together and document the decision.
>
> Do not write any code yet. As a first step, ask clarifying questions, list your assumptions, and propose 2–3 scope and technology options with their pros and cons. Then wait for my approval.

**Response:** a scoping proposal with 9 clarifying questions (each with a proposed default), assumptions, a note on which external APIs need keys, three options (A: JDK-only monolith, B: Docker Compose stack, C: B plus RabbitMQ) and a recommendation for B. See [`planning/01-scoping-proposal.md`](planning/01-scoping-proposal.md).

---

## Prompt 2 — Treefa, 10:37

> Please raise your questions and uncertainties (those that require an answer or discussion) interactively, one by one.

**Response:** the questions were reduced to 8 that needed a decision and asked one at a time as multiple-choice questions with a recommended option. Logged as [`review-log.md`](review-log.md) R1.

---

## Choice — Q1, 10:39

Run setup: **Docker Compose stack (Option B)**.

## Choice — Q2, 10:53

Event sources: **Simulator plus optional USGS**.

---

## Prompt 3 — Treefa, 10:56 (answer to Q3, login)

> Since this isn't a production project and we're short on time, let's go with option "A"

Option A = no login, demo user switcher.

## Choice — Q4, 10:59

Admin view: **Monitor and test**.

---

## Prompt 4 — Treefa, 11:11 (answer to Q5, alert rule filters)

> Going with A. Let's document C as a valid feature for the future. (Going forward, we'll sometimes take the faster route because of scope, time constraints, or the fact that this isn't a real product. We'll document all of these decisions, past and future, so they can be revisited and improved later!)

Option A = category, keywords, severity. Option C = region/location filter.

**Response:** started the trade-offs register ([`planning/03-tradeoffs-register.md`](planning/03-tradeoffs-register.md), now [`tradeoffs.md`](tradeoffs.md)), backfilled with every shortcut decided so far. Logged as [`review-log.md`](review-log.md) R2.

## Choice — Q6, 11:13

Delivery: **Immediate, dedup, 3 retries**.

---

## Prompt 5 — Treefa, 11:17 (answer to Q7, Git workflow)

> Going with A, since I'm the only developer on this project and I review and test the code before every commit and push.

Option A = direct commits to main.

**Response:** before asking Q8, Claude checked current versions live on Maven Central and npm instead of relying on memory ([`planning/04-version-check.md`](planning/04-version-check.md)).

## Choice — Q8, 11:20

Spring Boot line: **Spring Boot 4.1.3**.

**Response:** wrote up the agreed scope and stack for approval ([`planning/05-agreed-scope.md`](planning/05-agreed-scope.md)).

---

## Prompt 6 — Treefa, 11:29 (approval)

> I approve, but first check the Boot 4.1. If you're not sure, Boot 3.x is fine too, though I'm not sure how long it's supported.

**Response:** verified springdoc-openapi compatibility with Boot 4.1 and the support status of 3.5.x and 4.1.x; kept Boot 4.1.3 (see [`decisions.md`](decisions.md) ADR-010). Logged as [`review-log.md`](review-log.md) R3. Found that the development environment cannot create GitHub repositories and asked Treefa to create the empty repository (ADR-012). Prepared milestone M0.

---

## Prompt 7 — Treefa, 11:33

> The public world-event-alerts GitHub repository is ready.

**Response:** attached the repository and confirmed it is empty. The first push needs the Claude GitHub App to have access to the repository.

---

## Prompt 8 — Treefa, 12:00

> Remove the Claude-Session link from commit message: it's account-bound and broken for reviewers. Prompt history lives in docs/prompt-log.md.
> "Treefa" name is okay.
> Done, I installed the Claude GitHub App on Kisligi/world-event-alerts.

**Response:** removed the `Claude-Session` line from the proposed commit message and recorded the convention (review log R4, [`process.md`](process.md)); asked for the final go for the M0 commit.

---

## Prompt 9 — Treefa, 12:02 (approval of the M0 commit)

> go

**Response:** committed `b170acb` and pushed `main` (M0). Then implemented milestone M1: Spring Boot backend, user and alert rule REST API, Flyway schema, 28 tests including a Testcontainers PostgreSQL integration test. See [`dev-journal.md`](dev-journal.md).

---

## Prompt 10 — Treefa, 12:19 (before approving M1)

> Before I approve, push these changes to a separate branch called m1-review (do not touch main) so I can pull and test them locally.

**Response:** committed M1 as `a15f180` on the new branch `m1-review` and pushed it; `main` stayed at `b170acb`. Logged as [`review-log.md`](review-log.md) R5.

---

## Prompt 11 — Treefa, 12:35 (M1 review)

> Okay. I can see the code works, the tests pass, and Swagger is working.
> For every change, make sure you write meaningful tests and aim for as much test coverage as possible. But it's very important that these are real, meaningful tests, not ones written just to increase coverage. (For example, right now there's no test that covers the call to "handleInvalid".)

**Response:** added JaCoCo coverage reporting and 20 behaviour-focused tests; one of them found and led to fixing a timestamp precision bug. Logged as [`review-log.md`](review-log.md) R6; testing standard added to [`process.md`](process.md).

---

## Prompt 12 — Treefa, 12:43 (approval of the M1 test commit)

> Yes, should every future milestone also go to its own review branch first, because I want to test first locally.
> You can update the m1-review with the new changes.

**Response:** made the review branch a rule for every milestone in [`process.md`](process.md) and ADR-009, then committed the M1 test changes on `m1-review` and pushed.

---

## Prompt 13 — Treefa, 12:51 (approval of M1)

> Approve

**Response:** fast-forwarded `main` to `df7e3c9` (no new commit) and pushed. Implemented milestone M2 (events, sources, matching) on the `m2-review` branch. See [`dev-journal.md`](dev-journal.md).
