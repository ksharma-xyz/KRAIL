# Docs Gardener Audit — 2026-08-15

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the sixth run. It builds on the
[first run](https://github.com/ksharma-xyz/KRAIL/pull/1727) (merged), the
[second run](https://github.com/ksharma-xyz/KRAIL/pull/1732) (merged), the
[third run](https://github.com/ksharma-xyz/KRAIL/pull/1777) (merged), the
[fourth run](https://github.com/ksharma-xyz/KRAIL/pull/1782) (merged), and the
[fifth run](https://github.com/ksharma-xyz/KRAIL/pull/1807) (merged), and adds a
delta review of everything that changed since (`6207db8..HEAD`, 41 commits over
6 days) — the largest delta this audit has covered: a full on-device AI search
input + alert summary feature, two new capability modules, and a new
store-listing screenshot framework.

## Feedback ingestion

Searched `is:pr label:docs-gardener` (any state) against `ksharma-xyz/krail`:
five results, PR #1727 (first run), #1732 (second run), #1777 (third run),
#1782 (fourth run), and #1807 (fifth run), all merged. All five report zero
issue comments and zero reviews — no `charter:`-prefixed feedback on any of
them. Nothing to fold into the Steering Log this run; no prior rejection to
avoid re-proposing.

## Charter Part A drift

Performed this run via a shallow clone of `ksharma-xyz/krail-bff`, then diffed
its `.github/docs-gardener/CHARTER.md` Part A section (everything between the
`## Part A: Core Policy` and `## Part B: Repo Overrides` headings) against this
repo's Part A byte-for-byte. **No drift** — the two Part A sections are
identical (the only diff line is the Part B heading itself, which is
repo-specific by design and outside Part A).

## Delta review this run

`6207db8` (the fifth run's own delta endpoint, i.e. the state audited by PR
#1807) to `origin/main` (`e66426b`) is 41 commits over 6 days (2026-08-09 to
2026-08-15). Highlights: the full on-device AI search input + alert summary
feature (`652e125` and ~25 follow-on fix/feat commits), two new capability
modules (`core:speech-to-text`, `core:text-recognition`), the analytics
registry auto-sync bot going live (`#1809`, `#1810`), a new cross-platform
store-listing screenshot framework (`#1803`), a saved-trips park-and-ride fix
(`#1813`), and a GTFS data bump (`#1814`).

`git diff --stat 6207db8..origin/main -- '*.md'` touches 19 markdown files.
Two protected ledgers changed as expected
(`docs/ANALYTICS_REGISTRY_HANDOFF.md` from the registry-sync work — no action
possible or needed) and `CLAUDE.md` changed (protected, flagged only, see
below). The rest are reviewed below.

### New docs classified and verified

- **`feature/trip-planner/ui/AI_SEARCH_UX.md`** (`ux-contract`) — the
  "Where are you going?" surface's failure-mode table and its "nothing the
  model produces is ever shown" rule. Spot-checked the load-bearing claims:
  - `isFeatureEnabled` and `OpenInput` — both present in
    `feature/trip-planner/ui/src/commonMain/kotlin/.../search/ai/AiSearchInputUiState.kt`
    and `AiSearchInputViewModel.kt`, matching the doc's flag-gating claim.
  - `SavedTripsState.dateTimeSelectionItem` and `TripPlannerRoutes`'s
    `dateTimeSelectionItemJson` — both present, matching the doc's claim that
    a parsed time now travels via the route rather than being discarded.
  - The regression test `a place the model invented is never quoted back at
    the rider` exists in `AiSearchInputViewModelTest.kt`.
  - Verdict: **accurate**. The doc itself says "Both of the open defects
    recorded here are now fixed" — see the `CLAUDE.md` finding below, where
    that fix landed *after* CLAUDE.md's own summary of this doc was written.
- **`feature/trip-planner/ui/ALERT_SUMMARY_UX.md`** (`ux-contract`) — the
  on-device AI alert-summary card. Checked every class it names:
  `AiTextService`, `AlertSummaryViewModel`, `CollapsibleAlert.kt`,
  `FlagKeys` (for `ALERT_SUMMARY_ENABLED`), `AiWheelMark.kt`,
  `AiSpinAnimation.kt` — all present at the stated paths. Verdict:
  **accurate**.
- **`docs/SEARCH_QUERY_TELEMETRY_SPEC.md`** (`ux-contract`) — what may be sent
  about a typed search query. Checked `SearchQueryAnalytics.kt`,
  `SearchQueryAnalyticsRedaction.kt`, `MAX_ZERO_RESULT_QUERY_LENGTH`,
  `zeroResultQueryOrNull`, `resolveLocalZeroResultQuery`, and
  `FuzzyStopSearchEvalTest` — all present. Verdict: **accurate**. Note: this
  doc is not yet listed in `CHARTER.md`'s Part B "ux-contract docs" list
  (neither is `AI_SEARCH_UX.md` or `ALERT_SUMMARY_UX.md`, above) even though
  `CLAUDE.md`'s own "Per-feature UX rule docs" section already treats all
  three as binding specs. Not an action this run — Part B is a repo override
  a human edits, not something a `report-only` run touches — but worth a
  human syncing that list the next time Part B is edited.
- **`docs/ANALYTICS_REGISTRY_SYNC.md`** (`reference`) — how the
  Pending-to-Registered flip is automated. Checked
  `.github/workflows/analytics-registry-sync.yml` and
  `scripts/validate_flip_diff.py` — both exist. Verdict: **accurate**.
- **`core/speech-to-text/README.md`** (`reference`) — new module, explicitly
  self-described as "interface + module boundary only... both actuals always
  report Unavailable." Two broken references found (see Findings below).
- **`core/text-recognition/README.md`** (`reference`) — new module,
  self-described as "implemented on both platforms... not yet wired into
  `AiScreenshotExtractCard`." `AiScreenshotExtractCard` does not exist yet
  (`grep -rln "AiScreenshotExtractCard" --include="*.kt" .`: zero hits) — but
  the doc already says this itself ("not yet wired"), so this is a consistent
  forward-reference to planned work, not a stale claim. No action.
- **`docs/investigations/AI_SEARCH_INPUT_MODE.md`** (`investigation`) — the
  original exploration/architecture proposal. Now stale, see Findings below.
- **`docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md`** (`investigation`)
  — working notes for the iOS Foundation Models bridge. Now stale, see
  Findings below.
- **`store-listing/framework/README.md`**, **`AGENT-GUIDE.md`**,
  **`QA-CHECKLIST.md`** (`guide`) — app-agnostic screenshot-pipeline
  groundwork. Cross-checked every file the framework README's "repository
  contract" and the agent guide's read-order list name
  (`verify-listing.py`, `render-store-images.py`, `manifest.json`,
  `listing-qa.json`, `DECISIONS.md`, `capture-flows/`) against
  `store-listing/framework/` and `store-listing/krail/` — all present.
  Verdict: **accurate**.
- **`store-listing/krail/README.md`**, **`capture-flows/README.md`**
  (`guide`) — app-specific runbook and flow docs. Not individually verified
  line-by-line this run (low staleness risk: six days old, tooling-only,
  no behavioral claims about app code); flagged for a closer read in a
  future run if they age past this delta.
- **`store-listing/krail/DECISIONS.md`** (`ledger`) — "the durable feedback
  record for the approved 2026-08-09 KRAIL store listing set... Future
  sessions must read it before changing captures." Append-only feedback
  record, matches the `ledger` taxonomy definition exactly. Protected by
  classification (content never modified), though not yet on `CHARTER.md`'s
  explicit protected-files list — same non-blocking note as the ux-contract
  docs above.
- **`store-listing/krail/upload-ready/2026-08-09/README.md`** (`guide`) —
  three-line upload instructions for one dated artifact folder. Six days old,
  not a dated report/investigation under the taxonomy (no findings or
  analysis to go stale), no action.

### New findings

1. **`core/speech-to-text/README.md`** (priority 1: broken references) —
   links two paths that do not exist anywhere in the repo:
   - `docs/investigations/ai_search_input_mockup.html` —
     `find . -iname "ai_search_input_mockup.html"`: zero hits.
   - `ios_permission_must_request.md` (cited as "this project's... lesson") —
     `find . -iname "ios_permission_must_request.md"`: zero hits.

   Both may refer to session-local or externally-tracked artifacts (the same
   pattern as the still-open `TESTING.md` finding from the first run), but as
   written they are dead references in a committed, in-repo doc.

2. **`docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md`** (priority 4:
   trim) — Status section's branch table still reads "Branches, stacked, not
   yet raised as PRs," listing `feat/ai-text-service`,
   `feat/taj-alert-feedback-vote`, and others. All of this already merged to
   `main` in `652e125` (`feat(trip-planner): add the on-device AI search
   input and alert summary`) and its follow-on commits:
   - `core/ai-text/src/iosMain/kotlin/.../IosAiTextService.kt` on `main`
     already calls the real `AiTextBridge` (Foundation Models), not a stub.
   - `core/ai-text/src/swift/aiTextBridge/AiTextBridge.swift` exists on
     `main`.
   - `taj/src/commonMain/kotlin/.../components/AlertFeedbackVote.kt` (the
     component `feat/taj-alert-feedback-vote` was building) exists on `main`.

   This is the same pattern the third run found in
   `docs/investigations/IN_APP_REVIEW_TIMING.md` (still open, carried forward
   below): a Status section describing a branch stack as unmerged survives
   past the merge itself. Trim candidate: replace the branch table with a
   one-line "shipped in `652e125`" note; the rest of the doc (the technical
   walkthrough of the Swift shim, the simulator caveat) is still accurate and
   should stay.

3. **`docs/investigations/AI_SEARCH_INPUT_MODE.md`** (priority 4: trim) —
   Status line reads "build in progress... speech-to-text, OCR, and the
   ViewModel/navigation wiring that actually uses it are not yet built." The
   ViewModel/navigation wiring is now built and shipped:
   `AiSearchInputViewModel` (imports and is constructed with a
   `SpeechToTextService`), full-screen/dialog presentation
   (`feat(trip-planner): AI input as a full screen on phones, a dialog on
   tablets`), and home-screen wiring (`refactor(trip-planner): wire the home
   screen to the AI sheet`) are all present on `main`. Speech-to-text and OCR
   *capability* genuinely are still unbuilt (`core:speech-to-text` and
   `core:text-recognition` both self-report their platform actuals as
   stubs/unimplemented, per their READMEs above), so the doc is half right —
   but as written the Status line undersells how much of the vertical slice
   already shipped. Trim candidate: narrow the "not yet built" claim to the
   two capability modules specifically.

4. **`CLAUDE.md`** (protected, flagged only) — new instance of the same
   pattern as the existing Submodules finding (carried-forward #1, below).
   Its "Per-feature UX rule docs" entry for `AI_SEARCH_UX.md` still reads
   "the two open defects (flag-off dead button, parsed time discarded at
   navigation)" (`CLAUDE.md` line 388), but `AI_SEARCH_UX.md` itself now
   says "Both of the open defects recorded here are now fixed" (line 42).
   Confirmed by commit order: `CLAUDE.md`'s line was written in `25b5fb2`
   (`docs(trip-planner): hold the AI rules down with a test and a ledger`);
   the defect fixes landed afterward in `5b140a7` (`fix(trip-planner): with
   the feature off, there is no way in`), `9c36556`/`4109671` (carrying a
   chosen time through to the timetable), and `CLAUDE.md` was never touched
   again to match. No action possible — protected content.

5. **`docs/dimension-tokens-plan.md`** (no action, updated evidence) — Phase
   2 migration count re-run: `grep -rn "[0-9]\+\.dp\b" --include="*.kt" . |
   grep -v build | grep -v /tokens/` now returns **250** hits, up from 236 at
   the fifth run — expected, since this delta added several new `taj`
   components (`AiListeningIndicator.kt`, `AiThinkingIndicator.kt`,
   `AiWheelMark.kt`, `MicIcon.kt`, `StopIcon.kt`, gradient tokens) with their
   own raw `.dp` literals. Still not fully implemented, still no archive
   action per the prune criteria.

### Coverage

Two of the three new capability modules landed with their own README the same
delta that created them (`core:speech-to-text`, `core:text-recognition`) —
exactly what the coverage duty asks for, no gap. The third,
**`core/ai-text`**, has no README at all
(`find core/ai-text -name "*.md"`: zero hits) despite being the most mature
of the three — it already has real Android (ML Kit GenAI) and iOS (Foundation
Models) actuals, while its two sibling modules are still stubs. It sits at 7
Kotlin files, just under this charter's "roughly 10+ source files" coverage
threshold (`find core/ai-text -name "*.kt" | grep -v build | wc -l`), so it is
not added to the coverage-gap table below, but it is the one module in this
delta most worth watching: `docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md`
and `feature/trip-planner/ui/ALERT_SUMMARY_UX.md` both describe pieces of it,
but neither is a substitute for a module-level README the way
`core/speech-to-text/README.md` is for its module.

No other new source directories from this delta cross the 10-file threshold
without a doc — `feature/trip-planner/ui/.../search/ai/`,
`.../components/ai/`, and `.../alerts/summary/` are all sub-areas of the
already-documented `feature/trip-planner/ui` module and are covered by
`AI_SEARCH_UX.md` and `ALERT_SUMMARY_UX.md` above.

`fix(analytics): fire park_ride_user_facility when saved trips auto-sync Park
& Ride` (`#1813`) touches only `feature/park-ride` source and test files, no
doc — consistent with the carried-forward `feature/park-ride` coverage gap
below, not a new finding.

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
   incomplete; raw-`.dp` count now 250 (see updated evidence above).
6. **`docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`** (priority 2: archive) —
   still verified shipped against `AnalyticsEvent.kt`. This delta's analytics
   commit (`#1813`, park-ride facility ids) targets a different event
   entirely; nothing to re-check against this plan's four stop-label events.
7. **`feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md`** (no action) — PR3
   (`StopSearchListItem`/`labelSubtitle`) still not found in code
   (re-ran `grep -rn "labelSubtitle"`: zero hits).
8. **`feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`** (priority
   2: archive) — still self-marked shipped/superseded, still verified
   (`ManageStopLabelsSheet.kt` still absent from the tree).
9. **`iosApp/README.md`** (priority 1 / coverage gap) — still links
   `docs/ios-dsym-crashlytics.md`, which still does not exist.
10. **`docs/investigations/IN_APP_REVIEW_TIMING.md`** (priority 4: trim,
    found third run) — Status section still claims four
    `app-review`/`user-lifecycle-store` branches are "not yet raised as PRs";
    they merged to `main` back in July (see PR #1777 for the full `git log`
    evidence). Rest of the doc still verified accurate. Still a trim
    candidate, not archive.

`SECURITY.md` re-checked: still no staleness surface (external links only).

### Coverage gaps (carried forward)

Per the coverage duty (10+ source files, no README/doc), re-counted this run;
no doc was added for any of these since they were first flagged:

| Directory | Kotlin files | Doc? |
|---|---|---|
| `feature/park-ride` (found third run) | 19 | none — touched again this delta (#1813, an analytics fix) with no accompanying doc |
| `feature/track` (found first run) | 32 | none |
| `feature/departures` (found first run) | 27 | none |
| `discover` (found first run) | 16 | none |
| `feature/debug-settings` (found first run) | 14 | none |
| `core/app-review` (found third run) | 10 | none — `docs/investigations/IN_APP_REVIEW_TIMING.md` covers its design/rationale in detail; a short `core/app-review/README.md` pointing there plus summarizing module layout would still close the gap |
| `core/remote-config` (found first run) | 10 | none |

All seven counts are unchanged since the fifth run — none of this delta's 41
commits touched these directories except the one park-ride fix noted above.
`park-ride` remains the largest undocumented surface and, again this run, the
most recently touched by shipped code.

---

## Deferred items

Everything above remains deferred to a future `active`-mode run (or human
action), since `report-only` makes no doc changes by design.

## Proposed action queue for the next `active` run, in charter priority order

1. Fix broken links: `TESTING.md`, `iosApp/README.md`,
   `core/speech-to-text/README.md` (two dead references — confirm with a
   human whether `ai_search_input_mockup.html` and
   `ios_permission_must_request.md` should be committed, or the references
   removed).
2. Archive with tombstones: `docs/bff-integration-plan.md`,
   `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md`,
   `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md`.
3. Update index/README files to match the above archive moves.
4. Trim stale sections: `docs/ci_cd/ci-cd-architecture.md`'s
   `distribute-google-play-manual.yml` line;
   `docs/investigations/IN_APP_REVIEW_TIMING.md`'s Status table;
   `docs/investigations/IOS_FOUNDATION_MODELS_BRIDGE.md`'s "not yet raised as
   PRs" branch table; `docs/investigations/AI_SEARCH_INPUT_MODE.md`'s Status
   line.
5. Create small docs for the coverage gaps, `feature/park-ride` first (largest
   and most recently active undocumented surface), then `core/app-review`,
   then `feature/debug-settings`, then `feature/track`, `feature/departures`,
   `discover`, `core/remote-config`. Watch `core/ai-text` (currently 7 files,
   just under threshold) for a README once it grows or gains another platform
   integration.
