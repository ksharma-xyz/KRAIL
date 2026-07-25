# Docs Gardener Audit — 2026-07-25

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the third run. It builds on the
[first run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1727](https://github.com/ksharma-xyz/KRAIL/pull/1727), merged) and
[second run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1732](https://github.com/ksharma-xyz/KRAIL/pull/1732), merged), and adds a
delta review of everything that changed since (33 commits, `573c047..HEAD`).

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against `ksharma-xyz/krail`:
two results, PR #1727 (first run) and PR #1732 (second run), both merged.
Checked issue comments, review comments, and review-comment threads on both:
all empty (zero comments on either PR). Nothing to fold into the Steering Log
this run; no prior rejection to avoid re-proposing.

## Charter Part A drift

**Skipped**, as in the second run. This session's GitHub access is scoped to
`ksharma-xyz/krail` only. Per the tooling available to this session, adding a
sibling-repo clone requires an explicit, live user request, and this is an
unattended scheduled run with no live user turn to make one. Per the charter's
staleness protocol fallback ("If the clone is unavailable, skip and note
'cross-repo checks skipped'"), no Part A diff was performed. The first run
(2026-07-17) found no drift; that result is not reconfirmed here.

## New files classified this run

| File | Class | Notes / evidence |
|---|---|---|
| `docs/FEATURE_QUALITY_CHECKLIST.md` | guide | Pre-flight checklist referenced from `CLAUDE.md` ("Building a feature — read the checklist first"). Spot-checked every symbol/path it cites: `scripts/fullQualityChecks.sh`, `ThemeContrastTest.kt`, `ParkRideAvailabilityLoaderTest.kt`, `NavKeySerializationConfigTest.kt`, `SerializationConfig.kt`, `getForegroundColor`/`ensureMinimumContrast` (`taj/.../A11yColors.kt`) — all exist. No staleness. |
| `docs/USER_LIFECYCLE_STORE.md` | reference | Documents `:sandook`'s `UserLifecycleStore`. Verified: `LifecycleCounter` enum exists (`UserLifecycleStore.kt:73`), `RealAppStart.recordFirstInstallIfAbsent()` exists and is called from `RealAppstart.kt:20`, and the claimed consumer (`core/app-review`'s `RealAppReviewManager.kt`) does read `SAVED_TRIP_OPEN`-successor state and `daysSinceFirstInstall()`. No staleness. |
| `docs/investigations/IN_APP_REVIEW_TIMING.md` | investigation | See finding below — one stale section. |

## Finding: `docs/investigations/IN_APP_REVIEW_TIMING.md` — stale Status table

The doc's **Status** section (lines 7-19) reads: "Four branches, stacked, not
yet raised as PRs" and lists `user-lifecycle-store`, `app-review-wrapper`,
`app-review-eligibility`, `app-review-trigger` as unmerged branches.

Evidence this is now stale — all four have merged to `main`, days before this
run:

```
$ git log --format="%ai %h %s" | grep -i app-review
2026-07-24 20:06:49 +1000 c321522 refactor(app-review): remove debug-only review proof sheet
2026-07-24 19:06:13 +1000 ec82bbb refactor(app-review): keep engagement thresholds app-side, gate on one flag
2026-07-24 02:27:45 +1000 ca73b5e feat(app-review): trigger review on shared delight moments
2026-07-24 22:47:56 +1000 a8a77f9 test(app-review): use :core:testing canonical fakes for Flag and preferences
2026-07-22 08:06:25 +1000 69ea244 feat(app-review): gate review requests on engagement and Remote Config
2026-07-22 08:05:57 +1000 89f260a feat(app-review): add platform review request wrapper
2026-07-21 23:50:40 +1000 2e4941b feat(sandook): add user-lifecycle store for install date and counters
```

`core/app-review/` exists on `main` with 10 Kotlin files; `sandook/.../12.sqm`
exists; the doc's own commit history (`c321522`, `ec82bbb`, `ca73b5e`) shows it
was edited by the same PRs that merged the feature. The rest of the document
(FINAL DESIGN, Implemented gates, Architecture) is internally consistent with
current code — spot-checked `AppReviewThresholds.kt`: `MIN_SAVED_TRIPS = 2L`,
`MIN_ACCOUNT_AGE_DAYS = 3L`, `MIN_DAYS_BETWEEN_ASKS = 150L`, all matching the
doc's tables exactly — and `SAVED_TRIP_OPEN` is confirmed gone
(`grep -rn "SAVED_TRIP_OPEN" --include="*.kt" .` returns zero hits, matching
the doc's "Deleted in the rework" claim).

This is a **trim candidate, not an archive candidate**: the doc is a live
reference for an inert-but-shipped feature (compliance constraints, QA/testing
instructions, gate tables) still worth keeping, not a finished plan to retire.
Proposed action for a future `active` run: replace the Status table with a
one-line "shipped, flag off by default" note and keep everything from FINAL
DESIGN onward as-is.

## Delta review: other commits since the 2026-07-18 run

Beyond the three new docs above, `573c047..HEAD` added the `park-ride` feature
(`feature/park-ride`, 19 Kotlin files) and grew `core/app-review` (10 Kotlin
files) — both checked against coverage duties below. It also touched the
analytics contract (`c4b7e4b` added `analytics-events.json` as a shared
contract, `a787df2` immediately dropped it again along with the per-PR
analytics CI) and CI workflows (`f726db3`, `9ea65ea`, `7b8d4b6`, `9df8d10`,
added `codeql.yml` previously, no new workflow file this cycle). None of these
touch `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`'s claims — confirmed no
`AnalyticsEvent.kt` commit in this range mentions `StopLabel`
(`git log --oneline 573c047..HEAD -- '**/AnalyticsEvent.kt'` shows only
`park-ride`/`app-review`-related changes) — so finding 6 below is unaffected.
`CLAUDE.md`'s analytics section ("no contract file to keep in sync, no
per-PR analytics test") is already consistent with the net state after
`a787df2`; no drift there (protected file, flagged only if it had drifted).

## Coverage gaps (new this run)

Per the coverage duty (10+ source files, no README/doc):

| Directory | Kotlin files | Doc? |
|---|---|---|
| `feature/park-ride` | 19 | none |
| `core/app-review` | 10 | none — though `docs/investigations/IN_APP_REVIEW_TIMING.md` covers its design/rationale in detail; a short `core/app-review/README.md` pointing there plus summarizing the module layout would still close the gap per the coverage duty's letter |

Carried forward, unchanged since the first run (still no README/doc, file
counts not re-measured beyond confirming no doc was added):
`feature/track`, `feature/departures`, `discover`, `core/remote-config`.
`feature/debug-settings` (14 Kotlin files, no doc) also carried forward.

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
   still verified shipped against `AnalyticsEvent.kt` (unaffected by this
   run's analytics-contract churn, see delta review above).
7. **`feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md`** (no action) — PR3
   (`StopSearchListItem`/`labelSubtitle`) still not found in code
   (re-ran `grep -rn "labelSubtitle"`: zero hits).
8. **`feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`** (priority
   2: archive) — still self-marked shipped/superseded, still verified
   (`ManageStopLabelsSheet.kt` still absent from the tree).
9. **`iosApp/README.md`** (priority 1 / coverage gap) — still links
   `docs/ios-dsym-crashlytics.md`, which still does not exist.

`SECURITY.md` (classified `guide` in the second run) re-checked: still no
staleness surface (external links only).

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
