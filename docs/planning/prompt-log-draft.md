# Prompt log (draft, to become docs/prompt-log.md)

Recorded verbatim, in order. Each entry notes who wrote it and when (UTC).

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

**Response:** scoping proposal, see `01-scoping-proposal.md`.

---

## Prompt 2 — Treefa, 2026-11-20 10:37 UTC

> Please raise your questions and uncertainties (those that require an answer or discussion) interactively, one by one.

**Response:** questions are now asked one at a time as decision cards; answers are recorded in `02-scoping-answers.md`.

---

## Prompt 3 — Treefa, 2026-11-20 10:56 UTC (answer to the login question)

> Since this isn't a production project and we're short on time, let's go with option "A"

Note: decision-card taps for Q1 and Q2 are recorded in `02-scoping-answers.md`; typed answers are recorded here verbatim.

---

## Prompt 4 — Treefa, 2026-11-20 11:11 UTC (answer to the alert rule filter question)

> Going with A. Let's document C as a valid feature for the future. (Going forward, we'll sometimes take the faster route because of scope, time constraints, or the fact that this isn't a real product. We'll document all of these decisions, past and future, so they can be revisited and improved later!)

**Response:** created `03-tradeoffs-register.md`, a register of every shortcut taken for scope/time/non-product reasons, backfilled with Q1–Q5.

---

## Prompt 5 — Treefa, 2026-11-20 11:17 UTC (answer to the Git workflow question)

> Going with A, since I'm the only developer on this project and I review and test the code before every commit and push.

---

## Prompt 6 — Treefa, 2026-11-20 11:29 UTC (approval of the agreed scope)

> I approve, but first check the Boot 4.1. If you're not sure, Boot 3.x is fine too, though I'm not sure how long it's supported.

**Response:** verified springdoc 3.2.0 against Boot 4.1 and the support status of 3.5.x and 4.1.x (`04-version-check.md`); kept Boot 4.1.3.

Note: answers to Q6 (delivery) and Q8 (Spring Boot version) were decision-card taps, recorded in `02-scoping-answers.md`.
