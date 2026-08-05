# Docs Gardener Audit — 2026-08-01

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the fourth run. It builds on the
[first run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1727](https://github.com/ksharma-xyz/KRAIL/pull/1727), merged), the
[second run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1732](https://github.com/ksharma-xyz/KRAIL/pull/1732), merged), and the
[third run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1777](https://github.com/ksharma-xyz/KRAIL/pull/1777), merged), and adds a
delta review of everything that changed since (`aa3b25f..HEAD`, 4 commits over
7 days: three automated NSW GTFS data bumps, #1778/#1780/#1781, and one
dependency bump, #1779).

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against `ksharma-xyz/krail`:
three results, PR #1727 (first run), #1732 (second run), and #1777 (third
run), all merged. All three report `"comments":0`; no issue comments, review
comments, or `charter:`-prefixed review threads on any of them. Nothing to
fold into the Steering Log this run; no prior rejection to avoid re-proposing.

## Charter Part A drift

**Performed this run** (unlike the second and third runs, which skipped it —
sibling-repo access was available this time via `add_repo` +
`get_file_contents`, no full clone needed). Fetched
`ksharma-xyz/krail-bff`'s `.github/docs-gardener/CHARTER.md` at its default
branch HEAD and diffed its Part A section against this repo's Part A
byte-for-byte (Mission through PR conventions, both sections delimited by the
`## Part A: Core Policy` / `## Part B: Repo Overrides` headings). **No drift**
— the two Part A sections are identical.

## Delta review this run

`aa3b25f` (the third run's merge commit) to `origin/main` (`6ed9290`) is 4
commits over 7 days:

```
$ git log --format="%ai %h %s" aa3b25f..origin/main
2026-08-01 13:53:13 +0000 6ed9290 Update NSW GTFS data (stops + routes) (#1781)
2026-07-31 14:01:51 +0000 2054f5f Update NSW GTFS data (stops + routes) (#1780)
2026-07-27 05:55:48 +0000 9948380 build(deps): bump json from 2.19.8 to 2.19.9 (#1779)
2026-07-26 13:51:48 +0000 8f57039 Update NSW GTFS data (stops + routes) (#1778)
```

`git diff --stat aa3b25f..origin/main` touches exactly four files: two binary
GTFS blobs (`NSW_BUSES_ROUTES.pb`, `NSW_STOPS.pb`), `Gemfile.lock`, and
`SandookPreferences.kt` (the two GTFS version constants bumped from 66/39 to
69/42, nothing else). **Zero `.md` files changed, zero new source
directories added, zero `AnalyticsEvent.kt` changes.** No doc in the repo
newly qualifies for classification or staleness action from this delta —
every finding below is a re-verification of prior-run findings, not a new
one.

---

## Carried-forward findings from the 2026-07-17 run

Re-verified this run; all still apply verbatim.

1. **`CLAUDE.md`** (protected, flagged only) — "Submodules" section still
   describes the removed `krail-api-proto` git submodule; no `.gitmodules`
   file and no `krail-api-proto/` directory in the tree. No action possible
   (protected content).
2. **`TESTING.md`** (priority 1: broken link) — line 200 still links
   `.claude/plans/on-a-worktee-look-expressive-cat.md`, which still does not
   exist in the repo.
3. **`docs/bff-integration-plan.md`** (priority 2: archive) — still fully
   shipped and still describes the superseded submodule proto-distribution
   mechanism.
4. **`docs/ci_cd/ci-cd-architecture.md`** (priority 4: trim) — still lists
   `distribute-google-play-manual.yml` (line 42), which still does not exist
   in `.github/workflows/` (14 workflow files present, none named that).
5. **`docs/dimension-tokens-plan.md`** (no action) — Phase 2 migration still
   incomplete: `grep -rn "[0-9]\+\.dp\b" --include="*.kt" . | grep -v build |
   grep -v /tokens/` now returns 236 hits (was 153 at the first run — the
   codebase grew faster than the migration; still not fully implemented, so
   still no archive action per the prune criteria).
6. **`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`** (priority 2: archive) —
   still verified shipped against `AnalyticsEvent.kt`; zero `AnalyticsEvent.kt`
   commits since the third run (confirmed in the delta review above), so
   nothing to re-check.
7. **`feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md`** (no action) — PR3
   (`StopSearchListItem`/`labelSubtitle`) still not found in code
   (re-ran `grep -rn "labelSubtitle"`: zero hits).
8. **`feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`** (priority
   2: archive) — still self-marked shipped/superseded, still verified
   (`ManageStopLabelsSheet.kt` still absent from the tree).
9. **`iosApp/README.md`** (priority 1 / coverage gap) — still links
   `docs/ios-dsym-crashlytics.md`, which still does not exist.
10. **`docs/investigations/IN_APP_REVIEW_TIMING.md`** (priority 4: trim,
    found third run) — Status section (lines 7-19) still claims four
    `app-review`/`user-lifecycle-store` branches are "not yet raised as PRs";
    they merged to `main` on 2026-07-21/22/24, unchanged since the third run
    found this (see PR #1777 for the full `git log` evidence). Rest of the
    doc (FINAL DESIGN, gate tables, `AppReviewThresholds.kt` values,
    `SAVED_TRIP_OPEN` removal) still verified accurate. Still a trim
    candidate, not archive: replace the Status table with a one-line
    "shipped, flag off by default" note.

`SECURITY.md` (classified `guide` in the second run) re-checked: still no
staleness surface (external links only).

### Coverage gaps (carried forward)

Per the coverage duty (10+ source files, no README/doc), re-counted this run;
no doc was added for any of these since they were first flagged (confirmed by
the delta review above — zero `.md` files changed since the third run):

| Directory | Kotlin files | Doc? |
|---|---|---|
| `feature/park-ride` (found third run) | 19 | none |
| `feature/track` (found first run) | 32 | none |
| `feature/departures` (found first run) | 27 | none |
| `discover` (found first run) | 16 | none |
| `feature/debug-settings` (found first run) | 14 | none |
| `core/app-review` (found third run) | 10 | none — `docs/investigations/IN_APP_REVIEW_TIMING.md` covers its design/rationale in detail; a short `core/app-review/README.md` pointing there plus summarizing module layout would still close the gap per the coverage duty's letter |
| `core/remote-config` (found first run) | 10 | none |

`park-ride` is the largest undocumented surface and the newest.

---

## Deferred items

Everything above remains deferred to a future `active`-mode run (or human
action), since `report-only` makes no doc changes by design.

## Proposed action queue for the next `active` run, in charter priority order

1. Fix broken links: `TESTING.md`, `iosApp/README.md`.
2. Archive with tombstones: `docs/bff-integration-plan.md`,
   `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`,
   `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
3. Update index/README files to match the above archive moves.
4. Trim the stale `distribute-google-play-manual.yml` line from
   `docs/ci_cd/ci-cd-architecture.md`; trim the stale Status table in
   `docs/investigations/IN_APP_REVIEW_TIMING.md` (evidence above).
5. Create small docs for the two new coverage gaps
   (`feature/park-ride`, `core/app-review`) plus the carried-forward
   `feature/debug-settings` gap, in that order — `park-ride` is the largest
   undocumented surface (19 files) and the newest.
