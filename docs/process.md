# Development process

This project was built by Treefa (developer and reviewer) working with Claude (an AI coding assistant) in a single day. Treefa set the ground rules below at the start; they shape the whole repository.

## Ground rules (from the first prompt, summarized)

1. Commit and push at each major milestone with a meaningful commit message. A milestone is done when the code **compiles, all tests pass, and the related documentation is updated**.
2. Claude performs commits and pushes, but **only after Treefa's explicit approval each time**, based on a change summary and the proposed commit message. Nothing is created, committed, pushed, force-pushed, rewritten or reconfigured without approval.
3. **Every commit on `main` leaves the project in a working state**, so any commit is a safe rollback point.
4. Documentation lives in `docs/`; `README.md` stays in the root.
5. The **full prompt history** is part of the project ([`prompt-log.md`](prompt-log.md)). Every rejection, correction or change request is recorded in [`review-log.md`](review-log.md).
6. All materials from planning and development are kept (design documents, decision logs, drafts, intermediate results, screenshots): see [`planning/`](planning/).
7. An outside reader must be able to understand what was built, why and how, and **run it with a single command, without API keys or accounts**.
8. No secrets, keys or environment variables are ever committed.
9. Uncertainty is stated, never papered over (e.g. whether an external API exists or is free).
10. Everything is in English.

The verbatim text is the first entry in [`prompt-log.md`](prompt-log.md).

## Added during planning

- **Questions one at a time** (Prompt 2): open questions are asked and decided individually, not in a batch. See [`review-log.md`](review-log.md) R1.
- **Record every shortcut** (Prompt 4): whenever we take the faster route because of scope, time, or because this is not a real product, it goes into [`tradeoffs.md`](tradeoffs.md) so it can be revisited.

## How a milestone is delivered

1. Claude implements the milestone and runs the build and all tests.
2. Claude updates the related docs (README, architecture, decisions, trade-offs, logs).
3. Claude shows Treefa a change summary and the proposed commit message.
4. Treefa reviews and tests, then approves or asks for changes (changes are logged in `review-log.md`).
5. Only after approval: one commit directly on `main`, then push.

### Commit message convention

- Conventional prefix (`docs:`, `feat:`, `test:`, `build:` …), a short subject naming the milestone, and a body explaining what and why.
- Ends with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` to credit the AI assistant.
- No links to Claude sessions: they are account-bound and do not open for reviewers ([`review-log.md`](review-log.md) R4). The prompt history is in [`prompt-log.md`](prompt-log.md) instead.

Why direct commits to `main` and not pull requests: see [`decisions.md`](decisions.md) ADR-009.

## Planning phase timeline (2026-11-20, UTC)

| Time | Step |
|------|------|
| 10:24 | Brief and ground rules received |
| 10:25 | Claude proposed questions, assumptions and three options in one message ([`planning/01-scoping-proposal.md`](planning/01-scoping-proposal.md)) |
| 10:37 | Treefa asked for questions one at a time |
| 10:39–11:20 | Eight questions decided ([`planning/02-scoping-answers.md`](planning/02-scoping-answers.md)) |
| 11:20 | Current library versions checked live ([`planning/04-version-check.md`](planning/04-version-check.md)) |
| 11:29 | Scope approved, with a request to double-check Spring Boot 4.1 first |
| 11:35 | Boot 4.1 compatibility and support dates verified; M0 prepared |
