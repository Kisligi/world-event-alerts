# Review log

Every time Treefa rejected, corrected, or asked for changes to one of Claude's proposals: what Claude proposed, what Treefa changed, and why. Choices where Treefa simply picked Claude's recommended option are not changes and are listed in [`planning/02-scoping-answers.md`](planning/02-scoping-answers.md) instead.

Times are UTC on 2026-11-20.

---

## R1 — 10:37 — How clarifying questions are asked

- **Claude proposed:** all clarifying questions (9) in a single message, each with a default answer, followed by three options and a recommendation ([`planning/01-scoping-proposal.md`](planning/01-scoping-proposal.md)).
- **Treefa changed:** "Please raise your questions and uncertainties (those that require an answer or discussion) interactively, one by one."
- **Why:** not stated. Claude's reading: a batch of questions is hard to discuss properly in one reply; one at a time gives each decision its own discussion and a clear record.
- **Effect:** questions were reduced to the 8 that needed a decision and asked one at a time as multiple-choice questions with a recommendation. This became a working rule ([`process.md`](process.md)).

## R2 — 11:11 — Record deferred options and shortcuts

- **Claude proposed:** for alert rules, category + keywords + severity, with the region filter simply left out.
- **Treefa changed:** accepted the proposal, and added: "Let's document C [region filter] as a valid feature for the future", plus a standing rule to document every decision where we take the faster route, past and future.
- **Why:** "we'll sometimes take the faster route because of scope, time constraints, or the fact that this isn't a real product … so they can be revisited and improved later."
- **Effect:** created [`tradeoffs.md`](tradeoffs.md), backfilled with all earlier shortcuts (T1–T8), and kept up to date from now on.

## R3 — 11:29 — Verify Spring Boot 4.1 before approving

- **Claude proposed:** Spring Boot 4.1.3, with springdoc-openapi for Swagger UI and its Boot 4.1 compatibility "to be verified at scaffold time".
- **Treefa changed:** approved the scope on condition that Boot 4.1 is checked first; Boot 3.x acceptable if unsure, but unclear how long it is supported.
- **Why:** not to build on an unverified assumption (ground rule 9).
- **Effect:** verified before any code: springdoc-openapi 3.2.0 is built against Boot 4.1.2 (compatible); Spring calls 3.5.16 the last OSS release of 3.5.x, so 3.x is not a safer fallback. Kept 4.1.3. See [`decisions.md`](decisions.md) ADR-010 and [`planning/04-version-check.md`](planning/04-version-check.md).

## R4 — 12:00 — Commit message trailers

- **Claude proposed:** the M0 commit message ending with two attribution lines added by Claude's environment: `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` and `Claude-Session: <link to the Claude session>`.
- **Treefa changed:** remove the `Claude-Session` link; keep the rest.
- **Why:** "it's account-bound and broken for reviewers. Prompt history lives in docs/prompt-log.md."
- **Effect:** commit messages in this repository end with the `Co-Authored-By` line only. Recorded as a convention in [`process.md`](process.md).
