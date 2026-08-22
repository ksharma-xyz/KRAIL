# Docs Gardener Audit — 2026-08-22

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the seventh run. It builds on the
[first run](https://github.com/ksharma-xyz/KRAIL/pull/1727) (merged), the
[second run](https://github.com/ksharma-xyz/KRAIL/pull/1732) (merged), the
[third run](https://github.com/ksharma-xyz/KRAIL/pull/1777) (merged), the
[fourth run](https://github.com/ksharma-xyz/KRAIL/pull/1782) (merged), the
[fifth run](https://github.com/ksharma-xyz/KRAIL/pull/1807) (merged), and the
[sixth run](https://github.com/ksharma-xyz/KRAIL/pull/1852) (merged), and adds
a delta review of everything that changed since (`e66426b..8ea85bf`, 131
commits over 7 days) — the largest delta yet: a full rebuild of the "Ask
KRAIL" AI search surface into one input bar, a four-role theme-colour system,
a testing-doctrine split, new Maestro end-to-end infrastructure, and six new
bug-postmortem entries in a new `docs/learning/` log.

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against `ksharma-xyz/krail`:
six results, PR #1727 (first run), #1732 (second run), #1777 (third run),
#1782 (fourth run), #1807 (fifth run), and #1852 (sixth run), all merged.
Checked issue comments, review comments, and review threads directly on all
six (not just the PRs' own descriptions) — all six report zero on every
channel. No `charter:`-prefixed feedback anywhere. Nothing to fold into the
Steering Log this run; no prior rejection to avoid re-proposing.

## Charter Part A drift

Performed this run via `add_repo` (read access) + a shallow clone of
`ksharma-xyz/krail-bff`, then diffed its `.github/docs-gardener/CHARTER.md`
Part A section (everything between the `## Part A: Core Policy` and
`## Part B: Repo Overrides` headings) against this repo's Part A
byte-for-byte. **No drift** — the two Part A sections are identical (the
only diff line across the whole file is the Part B heading itself, which is
repo-specific by design and outside Part A).

## Delta review this run

`e66426b` (the sixth run's own delta endpoint, i.e. the state audited by PR
#1852) to `origin/main` (`8ea85bf`) is 131 commits over 7 days (2026-08-15 to
2026-08-22). Highlights: the Ask KRAIL AI search surface rebuilt around one
input bar (`8289ff6` and ~15 follow-on commits: suggestion sourcing from the
rider's own stops, a situations table, dropping an invented listening-ceiling
time, theme-painted AI surfaces), a theme-colour system split into four
explicit roles (ground/ink/background/decor, `3fd27c4` and follow-ons), a
large testing-doctrine expansion (`TESTING.md` split into
`docs/testing/{LAYERS,GUARDS,COVERAGE}.md`, new Maestro E2E smoke/nightly
flows, a Kover+Codecov coverage pipeline), six dated postmortems in a new
`docs/learning/` log, a new rider-facing `docs/marketing/` doc, and multiple
bug fixes (park-ride refresh cadence, rate limiting, polling-lifecycle
guards, layout/inset bugs).

`git diff --stat e66426b..8ea85bf -- '*.md'` touches 29 markdown files
(excluding this audit file itself). One protected file changed as expected
(`CLAUDE.md`, 105 lines — flagged only, see below); the two protected
ledgers (`docs/ANALYTICS_EVENTS.md`, `docs/ANALYTICS_REGISTRY_HANDOFF.md`)
and `docs/release-notes/**`/`.claude/**` were **not** touched this delta. The
rest are reviewed below.

### New and changed docs classified and verified

- **`taj/THEME_COLOUR_ROLES.md`** (`ux-contract`, new) — the four-role theme
  colour rule already referenced from `CLAUDE.md`. Verified all five
  accessors (`themeGroundColor`, `themeInkColor`, `themeInkColorOn`,
  `themeDecorColor`, `themeBackgroundColor`) at
  `taj/src/commonMain/kotlin/.../ColorsExt.kt`; `themeColor()` confirmed
  `@Deprecated(..., ReplaceWith("themeGroundColor()"))` in the same file;
  `ThemeColorRoleRule` confirmed at
  `gradle/build-logic/detekt-rules/.../ThemeColorRoleRule.kt`; the doc's
  `pastDepartureRowSurface` claim about `inkGrounds()`/`hardestInkGround()`
  matches `taj/.../theme/ThemeInk.kt` exactly. Verdict: **accurate**.
- **`feature/trip-planner/ui/ASK_KRAIL_UX.md`** (`ux-contract`, new) and
  **`ASK_KRAIL_MANUAL_TESTS.md`** (`guide`, new) — the rebuilt screen surface
  and its manual QA runbook. Every cited class/test
  (`AiSuggestionSituations.kt`, `AiGreeting.kt`, `LabelSynonyms.kt`,
  `AskKrailScreen.kt`, `AiInputBar.kt`'s `showWorkingBorder`,
  `closeAfterHandoff`/`isAiHandoffSettling`/`isHandoffActionable`, the test
  "the row-write gate closes with the dialog") found at the paths claimed.
  Verdict: **accurate**.
- **`feature/trip-planner/ui/AI_SEARCH_UX.md`** (14-line diff) — re-verified
  against `ASK_KRAIL_UX.md`: the two are **not** duplicates or a
  supersession. `AI_SEARCH_UX.md` covers the `search/ai/` extraction pipeline
  and the "nothing the model produces is shown" rule; `ASK_KRAIL_UX.md`
  covers the `components/ai/` screen surface, copy, and speech UX. Each
  explicitly cross-references the other at the top. No archive action
  warranted; both current.
- **`docs/testing/LAYERS.md`, `GUARDS.md`, `COVERAGE.md`** (`reference`, new)
  and the restructured **`TESTING.md`** — confirmed `TESTING.md`'s own claim
  (also echoed in `CLAUDE.md`) that it now points to these three files; all
  three exist, resolve, and are substantive (494/354/267 lines, not stubs).
  Verdict: **accurate**.
- **`docs/learning/README.md`** (`reference`, new, index) and six dated
  entries (`2026-08-16-clipped-inside-its-own-parent.md`,
  `2026-08-16-ime-pan-and-unbounded-column.md`,
  `2026-08-21-sheet-closed-by-a-text-callback.md`,
  `2026-08-22-a-lane-that-never-ran.md`,
  `2026-08-22-...three-contrast-guards-and-the-gap-between-them.md`,
  `2026-08-22-two-platforms-two-vocabularies.md`) — all six dated entries
  conform to the format the README itself specifies. Verdict: **accurate**.
- **`docs/marketing/ASK_KRAIL_CAPABILITIES.md`** (new) — a rider-facing
  capability summary for release notes / store listings, distinct from the
  ux-contract docs it links out to. **Does not fit any existing taxonomy
  label**: not `ledger` (not append-only), not `ux-contract` (not a binding
  team spec, it's external-facing copy), not `reference`/`guide`/`plan`/
  `investigation`. See Findings below — flagged as a taxonomy gap, no action
  taken per the charter's "propose an addition, do not act on it" rule.
- **`.maestro/README.md`** (`guide`, new) — `smoke/`, `nightly/`, `shared/`,
  `ci/run-flows.sh`, `config.yaml` all confirmed present and matching the
  doc's description. Verdict: **accurate**.
- **`docs/INTEGRATION_TESTING_PLAN.md`** (`plan`, new) — genuinely
  in-progress, not prune-eligible. Confirmed `AskKrailHandoffFlowTest.kt` and
  `SearchStopRoundTripFlowTest.kt` exist (marked done in the doc), while
  items 3 and 4 have no matching test file and are correctly left
  un-struck-through as still-proposed. Also confirmed
  `RealStopResultsManagerTest.kt` no longer exists, matching the doc's claim
  it was deleted as dead-weight coverage. Verdict: **accurate, correctly
  self-describes as partial**.
- **`docs/ANALYTICS_ASSUMPTIONS.md`** (`ledger`, new) — per `CLAUDE.md`'s own
  description of this file's purpose and format. Verdict: **accurate**.
- **`README.md`, `docs/FEATURE_QUALITY_CHECKLIST.md`,
  `docs/POLLING_LIFECYCLE.md`, `docs/TABLET_FOLDABLE_UX.md`,
  `feature/trip-planner/ui/SEARCH_STOP_UX.md`, `taj/README.md`** (small
  diffs, existing labels) — all changes self-consistent, no new staleness.
  Notably: `docs/POLLING_LIFECYCLE.md`'s additions
  (`PollingLifecycleGuardTest`, `TripPoller`,
  `ParkRideRefreshHelper.pollWhileCardsAreOpen`,
  `TimeTableRefreshPolicy.shouldAutoRefresh`,
  `DepartureBoardRepository.fetchIfWindowOpen`, `VirtualClock`) all verified
  present; `docs/TABLET_FOLDABLE_UX.md`'s additions
  (`DualPaneScaffold`/`DUAL_PANE_LIST_WIDTH`/`DualPaneScaffoldTest`,
  `RoutePaneMetadataTest`, new route rows) all verified present, and
  `ServiceAlertRoute`/`DateTimeSelectorRoute` confirmed genuinely deleted,
  matching the doc's "deleted (#1916)" claim;
  `feature/trip-planner/ui/SEARCH_STOP_UX.md`'s claim that
  `StopFilterByProductClassTest.kt` "has been removed" confirmed accurate.

### New findings

1. **`docs/marketing/ASK_KRAIL_CAPABILITIES.md`** (priority: taxonomy
   addition, not a code-verifiable staleness issue) — new content type not
   covered by the charter's seven-label taxonomy. Proposed new label:
   `marketing-copy` — evergreen, rider-facing capability/feature summaries
   for release notes and store listings; content-rule-gated (no numbers,
   per its own stated rule, same public-repo constraint as
   `docs/ANALYTICS_ASSUMPTIONS.md`); kept current as capability changes
   (unlike an append-only ledger); verified the same way as `reference` docs
   — code-level evidence for every capability claim. Flagged only, per
   charter step 4; no action taken this run.

No other new doc in this delta produced a priority 1-4 finding (no broken
links, no archive-eligible docs, no index fixes, no trim candidates) — every
other new or changed doc checked out accurate against current code.

### Coverage

Two directories crossed the roughly-10-file threshold in this delta, both
already covered by existing docs (same pattern prior runs used for
sub-areas of already-documented modules):

- `feature/trip-planner/ui/.../search/ai/resolve/` (14 Kotlin files) —
  covered by `AI_SEARCH_UX.md` and `ASK_KRAIL_UX.md` §5 (label resolution,
  synonyms, resolvers named and explained).
- `gradle/build-logic/detekt-rules/src/main` (10 Kotlin files) — covered by
  `docs/testing/GUARDS.md` lines 44-198, which names the module path and
  documents `ThemeColorRoleRule` specifically.

No new coverage gap this run.

---

## Carried-forward findings from the 2026-08-15 run

Re-verified this run with fresh grep/find/git-log evidence.

1. **`CLAUDE.md`** (protected, flagged only) — "Submodules" section (line
   246) still describes `krail-api-proto` as a live pulled-in submodule; no
   `.gitmodules` file and no `krail-api-proto/` directory exist. A caveat
   was added elsewhere in the file (line 294, under Worktree setup: "the
   repo no longer uses the `krail-api-proto` submodule") but the misleading
   section header itself is unchanged. No action possible (protected
   content).
2. **`TESTING.md`** (was priority 1: broken link) — **NOW FIXED**. The link
   to `.claude/plans/on-a-worktee-look-expressive-cat.md` did not survive
   this delta's restructure of `TESTING.md` into
   `docs/testing/{LAYERS,GUARDS,COVERAGE}.md`
   (`grep -rn "on-a-worktee-look-expressive-cat" .`: zero hits in
   `TESTING.md` or anywhere else in the tracked tree). Dropped from the
   carried-forward list.
3. **`docs/bff-integration-plan.md`** (priority 2: archive) — still fully
   shipped and still describes the superseded submodule proto-distribution
   mechanism as current (14 fresh grep hits for "submodule", unchanged).
4. **`docs/ci_cd/ci-cd-architecture.md`** (priority 4: trim) — still lists
   `distribute-google-play-manual.yml` (line 42); `.github/workflows/` still
   has only `distribute-google-play.yml`, no "-manual" variant.
5. **`docs/dimension-tokens-plan.md`** (no action) — raw-`.dp` literal count
   re-run: **257** (up from 250 at the sixth run), from this delta's new
   `taj` AI-input and theme-role components. Still not fully implemented,
   still no archive action per the prune criteria.
6. **`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`** (priority 2: archive) —
   re-confirmed shipped: all four named events
   (`stop_label_created`, `stop_label_stop_assigned`, `stop_label_removed`,
   `stop_label_reordered`) present in `AnalyticsEvent.kt` at lines 382, 421,
   460, 497.
7. **`feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md`** (no action) — PR3
   (`StopSearchListItem`/`labelSubtitle`) still not found in source
   (re-ran `grep -rn "labelSubtitle"`: only the plan doc itself and last
   run's audit file reference it; zero `.kt` hits).
8. **`feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`** (priority
   2: archive) — still self-marked `Status: **shipped.**`, still verified
   (`ManageStopLabelsSheet.kt` still absent from the tree).
9. **`iosApp/README.md`** (priority 1 / coverage gap) — still links
   `docs/ios-dsym-crashlytics.md` (line 7), which still does not exist.
10. **`docs/investigations/IN_APP_REVIEW_TIMING.md`** (priority 4: trim,
    escalating) — Status section still claims four
    `app-review`/`user-lifecycle-store` branches are "not yet raised as
    PRs." This is now *more* stale than at the third or sixth run: the
    feature is fully merged and live in production code, not just
    merged-as-a-branch — `core/app-review` exists, and its emitted event
    `review_prompt_requested` is present in `AnalyticsEvent.kt` (line 1625).
    `git log --oneline --all | grep -i app-review` shows the branches
    landed as ordinary merged commits (`feat(app-review): trigger review on
    shared delight moments`, `feat(app-review): gate review requests on
    engagement and Remote Config`, `refactor(app-review): ...`), not open
    branches. Same trim shape as the `IOS_FOUNDATION_MODELS_BRIDGE.md`
    finding two runs ago: replace the Status section with a one-line
    "shipped" note; the rest of the doc's rationale and design notes remain
    a live reference.

`SECURITY.md` not re-checked this run (no delta touched it; last verified
clean at the second run).

### Coverage gaps (carried forward)

Per the coverage duty (10+ source files, no README/doc), re-counted this
run:

| Directory | Kotlin files (last run) | Kotlin files (this run) | Doc? |
|---|---|---|---|
| `feature/track` (found first run) | 32 | **36** (+4) | none |
| `feature/departures` (found first run) | 27 | 27 | none |
| `feature/debug-settings` (found first run) | 14 | **15** (+1) | none |
| `discover` (found first run) | 16 | 16 | none |
| `feature/park-ride` (found third run) | 19 | 19 | none |
| `core/remote-config` (found first run) | 10 | **11** (+1) | none |
| `core/app-review` (found third run) | 10 | 10 | none — `docs/investigations/IN_APP_REVIEW_TIMING.md` covers its design/rationale in detail; a short `core/app-review/README.md` pointing there plus summarizing module layout would still close the gap |

`feature/track` remains the largest undocumented surface and grew again
this delta (theme/AI-surface touches to shared track UI). `core/remote-config`
and `feature/debug-settings` also grew slightly. None of the seven gained a
doc since first flagged.

**`core/ai-text`** (watch item since the sixth run) — still **7** Kotlin
files, unchanged, still no `*.md` in the directory, still just under the
~10-file threshold. Still worth watching, no action.

---

## Deferred items

Everything above remains deferred to a future `active`-mode run (or human
action), since `report-only` makes no doc changes by design. In addition:
the `marketing-copy` taxonomy proposal (new finding #1) needs a human
decision on whether to add it to `CHARTER.md`'s classification taxonomy —
a `report-only` run cannot edit the charter itself for this.

## Proposed action queue for the next `active` run, in charter priority order

1. Fix broken links: `iosApp/README.md` (`docs/ios-dsym-crashlytics.md` does
   not exist).
2. Archive with tombstones: `docs/bff-integration-plan.md`,
   `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`,
   `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
3. Update index/README files to match the above archive moves.
4. Trim stale sections: `docs/ci_cd/ci-cd-architecture.md`'s
   `distribute-google-play-manual.yml` line;
   `docs/investigations/IN_APP_REVIEW_TIMING.md`'s Status section (now the
   most out-of-date carried-forward finding — the feature it describes as
   unmerged branches has been live in production for weeks).
5. Create small docs for the coverage gaps, `feature/track` first (largest
   and still growing), then `feature/departures`, `feature/debug-settings`,
   `discover`, `feature/park-ride`, `core/remote-config`, then
   `core/app-review` (has a ready-made source doc to summarize/link).
   Watch `core/ai-text` (still 7 files) for a README once it crosses the
   threshold.

A human should also decide whether to add `marketing-copy` to `CHARTER.md`'s
classification taxonomy (new finding #1) before the next run, so
`docs/marketing/ASK_KRAIL_CAPABILITIES.md` (and any future doc like it)
stops being flagged as unclassifiable.
