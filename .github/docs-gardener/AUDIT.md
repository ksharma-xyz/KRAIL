# Docs Gardener Audit — 2026-07-18

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the second run of the docs gardener against this repository. It
builds on the [first run's findings](#carried-forward-findings-from-the-2026-07-17-run)
(PR [#1727](https://github.com/ksharma-xyz/KRAIL/pull/1727), merged,
unmodified below) and adds a delta review of everything that changed since.

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against
`ksharma-xyz/krail`: one result, PR #1727 (merged, first run). Checked its
issue comments, reviews, and review-comment threads: all empty (zero
comments). Nothing to fold into the Steering Log this run; no prior rejection
to avoid re-proposing.

## Charter Part A drift

**Skipped.** This session's GitHub access is scoped to `ksharma-xyz/krail`
only; a request for `ksharma-xyz/krail-bff`'s charter file was denied
("repository is not configured for this session"). Per the charter's
staleness protocol ("If the clone is unavailable, skip and note 'cross-repo
checks skipped'"), no Part A diff was performed this run. The first run
(2026-07-17) found no drift; that result is not reconfirmed here.

## Delta review: commits since the 2026-07-17 run

Four commits landed on `main` after PR #1727 merged (`658ce69`) and before
this run:

| Commit | Change | Doc impact checked |
|---|---|---|
| `05c8a84` (#1728) | Added `SECURITY.md` | New tracked doc — added to classification table below. |
| `a144e6e` (#1729) | Added `.github/workflows/codeql.yml` | Checked against `docs/ci_cd/ci-cd-architecture.md`'s workflow list (Finding §4, carried forward): that list is already a curated/illustrative subset of workflow files (it omits `analytics-ledger-guard.yml`, `lint-workflows.yml`, `release-1-cut.yml`, `release-2-deploy-rc.yml`, `bump-after-release.yml`, `create-github-release.yml`, `distribute-testflight.yml` — none of which are flagged), not an exhaustive registry, so `codeql.yml`'s absence is consistent with the doc's existing scope and is not a new staleness finding. Padding the list to be exhaustive is not the gardener's job (coverage duty explicitly warns against padding). |
| `f42690e` (#1730) | Removed the `PublicTransportNote` disclaimer row (and its `publicTransportNoteItem()` helper) from `SearchStopScreen.kt` | `grep -rn -i "public transport\|PublicTransportNote\|disclaimer" --include="*.md" .` matches only unrelated prose in `README.md` and `.claude/commands/krail-release-notes.md`. No doc (including the `ux-contract` override `SEARCH_STOP_UX.md`) described this disclaimer, so removing it created no doc staleness. |
| `573c047` (#1731) | Added a `BackHandler` so system back exits saved-trips edit mode instead of the app | Checked `docs/TABLET_FOLDABLE_UX.md` §4 (`SavedTripsScreen`, the only ux-contract doc covering this screen): it documents compact-height row adaptation and dual-pane wiring only, no back-press behavior, so nothing there was contradicted. No other doc (`SAVED_TRIPS_NAMING_PLAN.md` is about trip naming) claims back-press behavior for this screen. No coverage gap flagged — this is a small, self-contained interaction, not a substantial undocumented module. |

None of the four commits invalidate a prior finding or introduce a new one.

## New file classified this run

| File | Class | Notes / evidence |
|---|---|---|
| `SECURITY.md` | guide | Vulnerability-reporting runbook. Links checked: GitHub private-advisory URL and `mailto:hey@krail.app` are external/static, not verifiable against code — no code symbols or repo-relative paths referenced. No staleness surface. |

---

## Carried-forward findings from the 2026-07-17 run

Re-checked for continued validity; all still apply verbatim (spot-checked
the underlying files/symbols named below still exist/are still missing as
described — nothing above touched them).

1. **`CLAUDE.md`** (protected, flagged only) — "Submodules" section still
   describes the removed `krail-api-proto` git submodule; no `.gitmodules`
   or `krail-api-proto/` directory in the tree. No action possible
   (protected content).
2. **`TESTING.md`** (priority 1: broken link) — line 200 still links
   `.claude/plans/on-a-worktee-look-expressive-cat.md`, which still does not
   exist in the repo.
3. **`docs/bff-integration-plan.md`** (priority 2: archive) — still fully
   shipped (Phase A/B/C code present) and still describes the superseded
   submodule proto-distribution mechanism.
4. **`docs/ci_cd/ci-cd-architecture.md`** (priority 4: trim) — still lists
   `distribute-google-play-manual.yml`, which still does not exist in
   `.github/workflows/`.
5. **`docs/dimension-tokens-plan.md`** (no action) — Phase 2 migration still
   incomplete (not re-run this cycle; no commit since touched `*.dp` usage
   outside `tokens/`).
6. **`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`** (priority 2: archive) —
   still verified shipped against `AnalyticsEvent.kt`.
7. **`feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md`** (no action) — PR3
   (`StopSearchListItem`/`labelSubtitle`) still not found in code
   (re-ran `grep -rn "labelSubtitle"`: zero hits).
8. **`feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`** (priority
   2: archive) — still self-marked shipped/superseded, still verified
   (`ManageStopLabelsSheet.kt` still absent; successor components still
   present).
9. **`iosApp/README.md`** (priority 1 / coverage gap) — still links
   `docs/ios-dsym-crashlytics.md`, which still does not exist.

Coverage gaps from the first run (`feature/track`, `feature/departures`,
`feature/park-ride`, `discover`, `feature/debug-settings`, `core/remote-config`)
are unchanged: none of this run's four commits added a new undocumented
module or grew an existing gap's file count materially.

---

## Deferred items

Everything above remains deferred to a future `active`-mode run (or human
action), since `report-only` makes no doc changes by design. No new items
were added to the queue this run beyond classifying `SECURITY.md`, which
requires no action.

## Proposed action queue for the next `active` run, in charter priority order

Unchanged from the 2026-07-17 run:

1. Fix broken links: `TESTING.md`, `iosApp/README.md`.
2. Archive with tombstones: `docs/bff-integration-plan.md`,
   `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`,
   `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
3. Update index/README files to match the above archive moves.
4. Trim the stale `distribute-google-play-manual.yml` line from
   `docs/ci_cd/ci-cd-architecture.md`.
5. Create a small doc for `feature/debug-settings` (coverage gap, highest
   priority of the gaps listed in the first run).
