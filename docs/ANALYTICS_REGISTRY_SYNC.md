# Analytics registry sync — how the Pending -> Registered flip is automated

Read `docs/ANALYTICS_REGISTRY_HANDOFF.md` first — this doc explains the automation that
keeps its `Status` column current. It does not replace that file's own "How to use this
file" section.

## What's still manual (by design)

Adding a row is manual and stays that way: when a PR adds a new event name, or changes
params on an existing event, add a `Pending` row to
`docs/ANALYTICS_REGISTRY_HANDOFF.md` in the same PR. This is now stated directly in this
repo's `CLAUDE.md` under "Analytics events" — a Claude session (or a human) working on
`AnalyticsEvent.kt` should see it there without needing to already know this doc exists.

Marking param and user-property rows `Documented` is also manual — they have no
per-item registry surface on the KRAIL-Analytics side to check against, so nothing can
verify them automatically. Mark those by hand once their shape is final.

## What's automated: new-event rows only

A row for a **brand new event name** (`Event` column has `(NEW EVENT)`, or `Param(s)`
starts `NEW event:`) gets flipped from `Pending` to `Registered` without a human, once
KRAIL-Analytics has labelled it.

1. KRAIL-Analytics labels the event in `dashboard/lib/eventLabels.ts`.
2. Its `dispatch-registry-sync.yml` workflow (on push to that file, or the daily
   22:40 UTC backstop) computes which Pending new-event rows in KRAIL's ledger are now
   labelled, and fires a `repository_dispatch` at KRAIL naming them. Only event names
   cross the boundary — no metrics, no other data.
3. KRAIL's `analytics-registry-sync.yml` receives it, flips the matching row(s), and
   opens a PR labeled `analytics-sync`.
4. `scripts/validate_flip_diff.py` independently re-checks the actual git diff before
   that PR is even opened: exactly one file changed, no lines added or removed, every
   changed line differs from its old version by nothing except the `Pending` ->
   `Registered` substring. Anything else fails the job — no push, no PR.
5. The PR auto-merges. Nobody needs to look at it or click anything.

## Why this bot is allowed to auto-merge and no other bot here is

Every other automated PR in this repo (docs-gardener included) requires a human to
merge it — see the Pull Requests section of `CLAUDE.md`. This bot is a deliberate,
narrow exception: its only possible edit is one table cell, and step 4 above
mechanically guarantees that on every single run, independent of whatever the flip
script itself believes it did. That is a different risk profile to a bot that edits
prose. If this bot's scope ever grows beyond that one cell, remove the auto-merge
exception in `CLAUDE.md` and put a human back in the loop.

## Setup (one-time)

Nothing new on this side. The push/PR/merge steps authenticate as the existing
krail-gtfs-bot GitHub App (`KRAIL_BOT_APP_ID` / `KRAIL_BOT_PRIVATE_KEY`, already a
secret here for `bump-after-release.yml`) via `actions/create-github-app-token`, the
same pattern that workflow already uses. Needed because a PR authored by the built-in
`GITHUB_TOKEN` never triggers `pull_request`-scoped required checks (a standing GitHub
restriction), which would leave every auto-merge attempt permanently blocked on the
required `code-quality / detekt` check — a GitHub App installation token is a distinct
real actor and doesn't have that restriction.

`KRAIL_DISPATCH_TOKEN` — a fine-grained PAT, scoped to this repo only,
Contents: read/write, stored as a secret in the **KRAIL-Analytics** repo. This is what
lets that repo call `POST /repos/ksharma-xyz/KRAIL/dispatches`. It is a real write
credential on this repo, not a narrower "dispatch-only" grant — GitHub has no such
permission. Containment is this repo's branch protection on `main` (it can push a
branch, not merge to `main`), not the token's own scope.

Both tokens expire on whatever schedule you set when creating them. An expired token
fails the relevant workflow run loudly (non-zero exit), it does not fail silently.

## Known gap

If a second event gets labelled while the first flip PR is still open, the daily
backstop dispatches the growing set, and the branch-name dedup (keyed on the exact
sorted event-name set) doesn't recognize it as the same PR — it opens a second one
instead of superseding the first. Merging both in sequence can produce a conflict on
the first event's row. Low-stakes (worst case is a hand-resolved conflict on a
docs-only PR) since auto-merge means neither PR sits open for long in practice, but if
it starts happening in practice, the fix is closing superseded `analytics-sync` PRs
(any whose flipped set is a strict subset of a new dispatch) before opening a new one.
