# Docs Gardener Audit — 2026-08-08

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the fifth run. It builds on the
[first run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1727](https://github.com/ksharma-xyz/KRAIL/pull/1727), merged), the
[second run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1732](https://github.com/ksharma-xyz/KRAIL/pull/1732), merged), the
[third run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1777](https://github.com/ksharma-xyz/KRAIL/pull/1777), merged), and the
[fourth run](#carried-forward-findings-from-the-2026-07-17-run) (PR
[#1782](https://github.com/ksharma-xyz/KRAIL/pull/1782), merged), and adds a
delta review of everything that changed since (`6ed9290..HEAD`, 16 commits
over 7 days).

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against `ksharma-xyz/krail`:
four results, PR #1727 (first run), #1732 (second run), #1777 (third run),
and #1782 (fourth run), all merged. All four report zero issue comments, zero
review comments, and zero review threads — no `charter:`-prefixed feedback on
any of them. Nothing to fold into the Steering Log this run; no prior
rejection to avoid re-proposing.

## Charter Part A drift

Performed this run via `add_repo` (read access) + a shallow clone of
`ksharma-xyz/krail-bff`, then diffed its
`.github/docs-gardener/CHARTER.md` Part A section (everything between the
`## Part A: Core Policy` and `## Part B: Repo Overrides` headings) against
this repo's Part A byte-for-byte. **No drift** — the two Part A sections are
identical.

## Delta review this run

`6ed9290` (the fourth run's own delta endpoint, i.e. the state audited by
PR #1782) to `origin/main` (`0cb2c07`) is 16 commits over 7 days
(2026-08-01 to 2026-08-08):

```
$ git log --format="%ai %h %s" 6ed9290..origin/main
2026-08-08 23:50:35 +1000 0cb2c07 fix(saved-trips): prevent duplicate trip identities (#1802)
2026-08-08 08:40:29 +1000 2d69977 fix(ios): distribute TestFlight builds to tester groups from CI (#1800)
2026-08-07 22:17:29 +1000 aaaa226 fix(search): remove the address search stop-count gate and result cache (#1798)
2026-08-07 19:25:17 +1000 63105ea feat(analytics): report the address search gate on search_stop_query (#1794)
2026-08-07 19:02:58 +1000 5dbaa7c feat(search): gate address search on local stop match count (#1793)
2026-08-06 13:55:32 +0000 1dbe6d8 Update NSW GTFS data (stops + routes) (#1797)
2026-08-06 18:20:26 +1000 ab2ecb8 fix(park-ride): stop car parks reading above 100% full (#1791)
2026-08-06 00:16:49 +1000 d347793 docs-gardener: fourth audit (report-only) (#1782)
2026-08-05 23:26:06 +1000 bd2496f chore: bump version to 1.27.0 (#1789)
2026-08-05 23:03:27 +1000 2348b1e fix(scripts): read the version portably in cut-release.sh (#1790)
2026-08-05 22:40:06 +1000 1848bda docs(analytics): record the param caps and how to read window reporting (#1788)
2026-08-05 20:23:11 +1000 76f7a4f feat(analytics): report the window KRAIL runs in, and when it changes (#1787)
2026-08-05 19:56:02 +1000 285fb0a feat(analytics): record why the save-trip prompt ended (#1786)
2026-08-05 19:31:26 +1000 ea6a7b7 feat(analytics): carry searchSessionId through to load_timetable_click (#1785)
2026-08-05 19:10:07 +1000 2f9907c fix(analytics): send park and ride facility ids, mark truncated values (#1784)
2026-08-04 20:34:49 +1000 a7fbb95 fix(analytics): sanitize location ids and names in event params (#1783)
```

`git diff --stat 6ed9290..origin/main -- '*.md'` touches five markdown files:
this repo's own `AUDIT.md` (the fourth run's own PR), two protected ledgers
(`docs/ANALYTICS_EVENTS.md`, `docs/ANALYTICS_REGISTRY_HANDOFF.md` — expected
churn from the six `feat(analytics)`/`fix(analytics)` commits above, no
action possible or needed on protected content), and two docs verified below.

- **`feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md`** (ux-contract,
  updated in #1798 alongside the stop-count-gate revert it documents).
  Re-read start to finish against current code:
  - `AddressSearchCache` is claimed removed —
    `grep -rn "AddressSearchCache" --include="*.kt" .` (excluding `build/`):
    zero hits. Confirmed gone.
  - `search_stop_address_max_local_stops` is claimed removed from the Remote
    Config table — `grep -rln "search_stop_address_max_local_stops"
    --include="*.kt" .`: zero hits. Confirmed gone.
  - The three classes still documented in the table
    (`AddressSearchEligibility.kt`, `AddressSearchQueryNormalizer.kt`,
    `AddressSearchMinQueryLength.kt`) all still exist at the stated path.
  - `STOPS_ALREADY_SUFFICIENT`/`CACHE_HIT` are described as historical-only
    enum values that "may still carry" in old rows — `AnalyticsEvent.kt`
    line 292 documents the same thing in its own comment, consistent.
  - Verdict: **accurate**, no action. This is the standard this charter asks
    for — the doc and the revert landed in the same PR.
- **`sandook/Migrations.md`** (reference; pre-existing since #1728-era work,
  not previously carried in this audit's classification) — extended in
  #1802 with a new "Saved-trip identity invariant" section. Verified against
  current code:
  - `RealSandook.insertOrReplaceTrip` derives `canonicalTripId` from
    `fromStopId`/`toStopId` as claimed (does not trust the caller-supplied
    `tripId`).
  - `KrailSandook.sq` has both `CHECK (tripId = fromStopId || '->' ||
    toStopId)` and `UNIQUE (fromStopId, toStopId)` on `SavedTrip`, matching
    the doc's constraint description.
  - Migration `13.sqm` exists and rebuilds the table with those constraints,
    matching the doc's migration guidance.
  - Verdict: **accurate**, no action. Newly brought into this audit's
    tracked set; classified `reference`.

`fix(park-ride): stop car parks reading above 100% full` (#1791) touches only
`feature/park-ride` source and test files, no doc — consistent with the
carried-forward `feature/park-ride` coverage gap below, not a new finding.

No new coverage-gap directories, no new broken links, no new plan/investigation
docs this run. Every finding below is a re-verification of a prior-run
finding, not a new one.

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
   in `.github/workflows/`.
5. **`docs/dimension-tokens-plan.md`** (no action) — Phase 2 migration still
   incomplete: `grep -rn "[0-9]\+\.dp\b" --include="*.kt" . | grep -v build |
   grep -v /tokens/` still returns 236 hits (unchanged since the fourth run —
   no `.kt` files in scope of that grep changed this delta), so still not
   fully implemented and still no archive action per the prune criteria.
6. **`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`** (priority 2: archive) —
   still verified shipped against `AnalyticsEvent.kt`. This delta added six
   analytics commits, but all target `search_stop_query`,
   `load_timetable_click`, save-trip-prompt, and window-report events, none
   of the four stop-label events this plan governs (confirmed by reading
   the plan's event table, reproduced in the delta review above). Nothing to
   re-check.
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
    they merged to `main` back in July, unchanged since the third run found
    this (see PR #1777 for the full `git log` evidence). Rest of the doc
    still verified accurate. Still a trim candidate, not archive: replace
    the Status table with a one-line "shipped, flag off by default" note.

`SECURITY.md` re-checked: still no staleness surface (external links only).

### Coverage gaps (carried forward)

Per the coverage duty (10+ source files, no README/doc), re-counted this run;
no doc was added for any of these since they were first flagged:

| Directory | Kotlin files | Doc? |
|---|---|---|
| `feature/park-ride` (found third run) | 19 | none — touched again this delta (#1791, a bug fix) with no accompanying doc |
| `feature/track` (found first run) | 32 | none |
| `feature/departures` (found first run) | 27 | none |
| `discover` (found first run) | 16 | none |
| `feature/debug-settings` (found first run) | 14 | none |
| `core/app-review` (found third run) | 10 | none — `docs/investigations/IN_APP_REVIEW_TIMING.md` covers its design/rationale in detail; a short `core/app-review/README.md` pointing there plus summarizing module layout would still close the gap per the coverage duty's letter |
| `core/remote-config` (found first run) | 10 | none |

`park-ride` remains the largest undocumented surface and is now also the
most recently touched by shipped code.

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
5. Create small docs for the coverage gaps, `feature/park-ride` first (largest
   and most recently active undocumented surface), then `core/app-review`,
   then `feature/debug-settings`.
