# Docs Gardener Audit — 2026-07-17

Run mode: **report-only** (per Part B of `CHARTER.md`). No documentation was
modified, moved, or deleted by this run. Everything below is a proposal for a
human (or a future `active`-mode run) to act on.

This is the first run of the docs gardener against this repository (charter
created 2026-07-15).

## Feedback ingestion

`gh`/GitHub search for `is:pr label:docs-gardener` and for `head:docs-gardener`
against `ksharma-xyz/krail` both returned zero results (the `docs-gardener`
label exists on the repo, so the query itself is valid — there are just no
prior PRs). Nothing to ingest; no Steering Log rejections apply; no new
Steering Log entries added this run.

## Charter Part A drift

Fetched `ksharma-xyz/krail-bff`'s `.github/docs-gardener/CHARTER.md` and diffed
its Part A section byte-for-byte against this repo's canonical copy: **no
drift**. `diff` returned no differences.

## Sibling-repo findings

Read-only check only (KRAIL-BFF's charter, for the Part A diff above). No
sibling-repo doc changes proposed or made; KRAIL-BFF's own Part B is
independently governed by its own gardener run.

---

## Classification table

Every tracked `*.md` file, one row each. `CHARTER.md` and this `AUDIT.md` are
the gardener's own operating files and are excluded. `.github/PULL_REQUEST_TEMPLATE.md`
is repo config (a GitHub feature file, not documentation) and is out of the
gardener's mission scope — listed for completeness, no classification applied.

| File | Class | Notes / evidence |
|---|---|---|
| `.claude/commands/cut-release.md` | guide | Protected (`.claude/**`). No action. |
| `.claude/commands/krail-release-notes.md` | guide | Protected (`.claude/**`). No action. |
| `.claude/skills/pr-desc/SKILL.md` | guide | Protected (`.claude/**`). No action. |
| `.github/PULL_REQUEST_TEMPLATE.md` | *(out of scope)* | Repo config, not documentation. No action. |
| `CLAUDE.md` | reference | Protected, content never modified. **Stale content found — see Findings §1.** Flagged only; cannot act (protected). |
| `README.md` | reference | Spot-checked, no broken links or stale symbols found. |
| `TESTING.md` | guide | **Broken link — see Findings §2.** |
| `core/app-start/README.md` | reference | 3 lines, clean. |
| `core/maps/data/LOCATION_PERMISSION.md` | reference | No stale symbols found. |
| `core/share/README.md` | reference | No stale symbols found. |
| `core/snapshot-testing-annotations/README.md` | reference | No stale symbols found. |
| `core/snapshot-testing/README.md` | reference | No stale symbols found. |
| `docs/ANALYTICS_EVENTS.md` | ledger | Protected, absolute no-touch. |
| `docs/ANALYTICS_REGISTRY_HANDOFF.md` | ledger | Protected, absolute no-touch. |
| `docs/MAPLIBRE_IOS_PITFALLS.md` | reference | Verified current: `SearchStopMap.kt`, `JourneyMap.kt`, `DualPaneScaffold.kt` all exist at the referenced paths. |
| `docs/POLLING_LIFECYCLE.md` | ux-contract | Not deep-verified this run (time budget); no issues surfaced incidentally. |
| `docs/TABLET_FOLDABLE_UX.md` | ux-contract | Not deep-verified this run; no issues surfaced incidentally. |
| `docs/archive/README.md` | archive-index | Tombstone ledger; append-only by protocol. No action. |
| `docs/bff-integration-plan.md` | plan | **Shipped / stale submodule mechanism — see Findings §3.** |
| `docs/ci_cd/ci-cd-architecture.md` | reference | **Stale workflow reference — see Findings §4.** |
| `docs/dimension-tokens-plan.md` | plan | **Partially implemented — see Findings §5.** No action (not fully shipped). |
| `docs/investigations/ADDRESS_ORIGIN_TRIP_INVESTIGATION.md` | investigation | Open/unresolved per its own "Next step" section. No action. |
| `docs/investigations/IOS_MODAL_BOTTOM_SHEET_FULL_EXPAND_INVESTIGATION.md` | investigation | Open; fix attempt logged as "not yet manually verified." No action. |
| `docs/investigations/NSW_715_WALK_LEG_INVESTIGATION.md` | ux-contract (override) | Verified current: `collapseSameRouteQuickWalks()` confirmed implemented in `TripResponseMapper.kt`. No action. |
| `docs/lint.md` | guide | No stale symbols found. |
| `docs/plans/ADDRESS_LOCATION_LABELS_PLAN.md` | plan | Self-marked "Exploratory design only... makes no runtime or schema change." Active, no action. |
| `docs/plans/ADDRESS_SEARCH_OBSERVABILITY_AND_ROLLOUT.md` | plan | Self-marked "Exploratory design only." Active, no action. |
| `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md` | plan | **Shipped, verified — see Findings §6.** |
| `docs/release-notes/README.md` | ledger | Protected (`docs/release-notes/**`). |
| `docs/release-notes/v1.20.0.md` | ledger | Protected. |
| `docs/release-notes/v1.22.0.md` | ledger | Protected. |
| `docs/release-notes/v1.23.0.md` | ledger | Protected. |
| `docs/release-notes/v1.24.0.md` | ledger | Protected. |
| `docs/release/RELEASE_PROCESS.md` | guide | Verified current: all 5 referenced workflow files (`release-1-cut.yml`, `release-2-deploy-rc.yml`, `create-github-release.yml`, `distribute-testflight.yml`, `bump-after-release.yml`) exist in `.github/workflows/`. |
| `feature/trip-planner/ui/ADDRESS_SEARCH_ELIGIBILITY.md` | ux-contract (override) | Not deep-verified this run beyond incidental reads; no issues surfaced. |
| `feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md` | plan | **Partially implemented — see Findings §7.** No action (not fully shipped). |
| `feature/trip-planner/ui/SAVED_TRIPS_NAMING_PLAN.md` | plan | Proposal only; `RenameTrip`/`RemoveSavedTrip` not found anywhere in code (`grep -rl` zero hits). Active, no action. |
| `feature/trip-planner/ui/SEARCH_STOP_UX.md` | ux-contract (override) | Not deep-verified this run beyond incidental reads; no issues surfaced. |
| `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md` | plan | **Shipped and superseded, verified — see Findings §8.** |
| `feature/trip-planner/ui/docs/fuzzy_stop_search.md` | reference | No stale symbols found (spot check). |
| `feature/trip-planner/ui/docs/timetable_cache_architecture.md` | reference | No stale symbols found (spot check). |
| `gradle/README.md` | reference | No stale symbols found. |
| `info-tile/network/real/src/commonTest/resources/INFO_TILE_VALIDATOR_README.md` | reference | Not deep-reviewed (test-resource doc, low risk). No action. |
| `io/gtfs/BUS_ROUTES_ARCHITECTURE.md` | reference | Verified current: schema described (`NswBusRouteGroups`/`NswBusRouteVariants`/`NswBusTripOptions`/`NswBusTripStops`) matches `sandook/.../NswBusRoutes.sq` exactly, table-for-table, column-for-column. |
| `io/gtfs/README.md` | reference | No stale symbols found. |
| `iosApp/README.md` | reference | **Broken link / missing doc — see Findings §9.** |
| `platform/README.md` | reference | 6 lines, clean. |
| `sandook/Migrations.md` | reference | Consistent with actual `sandook/` migration conventions observed. No action. |
| `sandook/README.md` | reference | No stale symbols found. |
| `taj/README.md` | reference | No stale symbols found. |

---

## Findings (staleness evidence + proposed actions, priority order)

### 1. `CLAUDE.md` — stale Submodules section (flagged, no action — protected)

`CLAUDE.md`'s "Submodules" section states KRAIL pulls in `krail-api-proto` as a
git submodule at `krail-api-proto/` and instructs `git submodule update --init
--recursive` for a fresh checkout. This is no longer true:

```
$ git log --all --oneline -- .gitmodules
fbed9fe build(proto): consume api-proto from GitHub Packages; remove submodule (#1683)
7fb7725 chore(ci): remove github.token smoke test workflow (#1675)
```

There is no `.gitmodules` file and no `krail-api-proto/` directory in the
working tree. `io/bff-api/build.gradle.kts` now resolves the proto sources
from a GitHub Packages artifact (`libs.krail.api.proto`) instead:

```kotlin
// Proto sources JAR from GitHub Packages — published by KRAIL-API-PROTO on each tag.
val krailProto: Configuration by configurations.creating { isTransitive = false }
dependencies {
    krailProto(libs.krail.api.proto) { artifact { classifier = "proto" } }
}
```

`CLAUDE.md` is protected content (absolute no-touch per Part B), so this run
takes no action. Flagging for the human maintainer since a fresh-checkout
contributor following the current instructions would run a no-op submodule
command and could be confused about where the proto sources actually come
from.

### 2. `TESTING.md` — broken link (priority 1: fix broken links)

Line 200:

```
- Plan that drove this work: [`.claude/plans/on-a-worktee-look-expressive-cat.md`](.claude/plans/on-a-worktee-look-expressive-cat.md) (in the parent checkout).
```

`.claude/plans/on-a-worktee-look-expressive-cat.md` does not exist anywhere in
the repository. The doc itself acknowledges it lives "in the parent
checkout" (i.e., a session-local Claude Code plan file, never committed) — so
this link can never resolve for any other clone or contributor. Proposed
action: remove the link or reword the bullet to note the plan was
session-local and not preserved in the repo.

### 3. `docs/bff-integration-plan.md` — shipped; also describes a superseded mechanism (priority 2: archive)

This ~880-line draft plan (dated 2026-05-10, "v2") describes Phase A, Phase B
(`feature/debug-settings`), and Phase C foundation work for BFF integration.
Verified against the current codebase:

```
$ find . -iname "BffEndpointResolver.kt"
./core/network/src/commonMain/kotlin/xyz/ksharma/krail/core/network/BffEndpointResolver.kt
$ ls -d feature/debug-settings
feature/debug-settings
$ find . -iname "JourneyListMapper.kt"
./feature/trip-planner/network/.../mapper/JourneyListMapper.kt
```

Phase A (BFF pass-through wiring) and Phase B (`feature/debug-settings/{state,store,ui}`,
`NetworkSource` enum, `BffEndpointResolver`) are both fully present in code —
every named file/type in the plan's "Status of the current branch" and
"Architecture (final...)" sections exists. Phase C's consumer mapper
(`JourneyListMapper.kt`) also exists.

Additionally, the plan's entire "Sharing proto contracts with KRAIL-BFF"
section (recommends a git submodule for `krail-api-proto`) describes a
mechanism the codebase no longer uses — see Finding §1 (`#1683` replaced the
submodule with a GitHub Packages artifact).

Proposed action: archive to `docs/archive/` with a tombstone in
`docs/archive/README.md` noting it was superseded by the shipped
`feature/debug-settings` module and the GitHub Packages proto-distribution
mechanism.

### 4. `docs/ci_cd/ci-cd-architecture.md` — one stale workflow reference (priority 4: trim)

The "Design Philosophy → Separation of Concerns" section lists
`distribute-google-play-manual.yml` as one of the repo's workflow files. It
does not exist:

```
$ ls .github/workflows/
analytics-ledger-guard.yml  build-android.yml  build-ios.yml  build.yml
bump-after-release.yml      code-quality.yml   create-github-release.yml
distribute-firebase.yml     distribute-google-play.yml  distribute-testflight.yml
lint-workflows.yml          release-1-cut.yml  release-2-deploy-rc.yml
```

All other workflow files the doc names (`build.yml`, `code-quality.yml`,
`build-android.yml`, `build-ios.yml`, `distribute-firebase.yml`,
`distribute-google-play.yml`) do exist. Proposed action: drop the
`distribute-google-play-manual.yml` line from that list (single-line trim).

### 5. `docs/dimension-tokens-plan.md` — Phase 1 shipped, Phase 2 migration incomplete (no action)

All six "File Creation Checklist" token files exist, matching the plan almost
exactly (implementation went slightly further than the plan, e.g. extra
tokens on `KrailDimensions`):

```
$ find taj -iname "*Tokens.kt"
taj/.../tokens/TextFieldTokens.kt  ComponentTokens.kt  StrokeTokens.kt
taj/.../tokens/ButtonTokens.kt     ContentAlphaTokens.kt  SpacingTokens.kt
taj/.../tokens/RadiusTokens.kt     IconSizeTokens.kt
$ grep -n "dimensions" taj/.../theme/Theme.kt
38:    val dimensions: KrailDimensions
```

But Phase 2 (module-by-module dp-to-token migration) is not complete. Running
the plan's own validation grep verbatim:

```
$ grep -rn "= [0-9]\+\.dp\|([0-9]\+)\.dp\|[0-9]\+\.dp)" --include="*.kt" \
    --exclude-dir="tokens" feature/ taj/components/ discover/ composeApp/ | wc -l
153
```

The plan expects this to return zero once migration is done. Since not every
checklist item is verifiably implemented, this does not qualify for archiving
under the prune criteria. No action; recorded here so a future run doesn't
need to re-derive this.

### 6. `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md` — shipped, verified (priority 2: archive)

Self-marked: *"Implemented 2026-07-15. All four events reshaped to the bounded
params below..."* Verified against `core/analytics/.../AnalyticsEvent.kt`: all
four events and their exact bounded parameters are present.

```
$ grep -n "stop_label" core/analytics/.../AnalyticsEvent.kt
310:        name = "stop_label_created",
349:        name = "stop_label_stop_assigned",
388:        name = "stop_label_removed",
425:        name = "stop_label_reordered",
```

`StopLabelCreatedEvent` carries `creationSurface`/`labelCountBucket`;
`StopLabelStopAssignedEvent` carries `assignmentSurface`/`assignmentMode`/
`locationKind`/`labelKind`/`isReassignment`; `StopLabelRemovedEvent` carries
`action`/`hadStop`/`labelKind`; `StopLabelReorderedEvent` carries
`labelKind`/`moveDistanceBucket`/`setLabelCountBucket` — matching the plan's
"Recommended event model" table field-for-field. Qualifies under prune
criteria ("self-marked done/shipped" and "plan whose checklist items are all
verifiably implemented"). Proposed action: archive with a tombstone noting the
doc's own text that it remains as "the measurement-model rationale" (so the
tombstone should point readers to `EVENT_REGISTRY.md` in KRAIL-Analytics for
the registered param list, per the doc's own final line).

### 7. `feature/trip-planner/ui/LABEL_DISPLAY_PLAN.md` — 2 of 3 PRs shipped (no action)

- PR1 (`karan/stop-display-model`) shipped: `StopDisplay.kt` exists at the
  planned path.
- PR2 (`karan/origin-destination-display-model`) shipped: `OriginDestination`
  now takes two `StopDisplay` params plus click handlers, matching the plan's
  refactored API exactly:
  ```
  internal fun OriginDestination(
      origin: StopDisplay, destination: StopDisplay, timeLineColor: Color,
      modifier: Modifier = Modifier,
      onOriginClick: ((StopDisplay) -> Unit)? = null,
      onDestinationClick: ((StopDisplay) -> Unit)? = null, ...
  )
  ```
- PR3 (`karan/recents-and-search-labels`) not shipped: no `StopSearchListItem.kt`
  file exists anywhere in the repo, and `grep -rn "labelSubtitle"` across the
  whole tree returns zero hits.

Not all checklist items are implemented, so this does not qualify for
archiving. No action; recorded for a future run.

### 8. `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md` — shipped and superseded (priority 2: archive)

Self-marked: *"Status: shipped... the v2 `ManageStopLabelsSheet.kt` referenced
below has been deleted. See `SEARCH_STOP_UX.md` for the current, up-to-date
description of what's live — this doc is kept as a historical design
record."* Verified:

```
$ find . -iname "ManageStopLabelsSheet.kt"
(no output — file does not exist)
$ find . -iname "ManageStopLabelsEntry.kt" -o -iname "ManageStopLabelsScreen.kt" -o -iname "StopLabelAssignRow.kt"
./feature/trip-planner/ui/.../navigation/entries/ManageStopLabelsEntry.kt
./feature/trip-planner/ui/.../managestoplabels/ManageStopLabelsScreen.kt
./feature/trip-planner/ui/.../searchstop/StopLabelAssignRow.kt
```

All components the doc claims shipped are present; the component it claims
was deleted is in fact absent. Clean match for the "self-marked done/shipped/
superseded" prune criterion, and the doc explicitly names its own successor
(`SEARCH_STOP_UX.md`). Proposed action: archive with a tombstone pointing to
`SEARCH_STOP_UX.md` as superseding doc (per the doc's own text).

### 9. `iosApp/README.md` — broken link to a doc that doesn't exist (priority 1 / coverage gap)

```
📖 **[iOS dSYM Files and Firebase Crashlytics Guide](../docs/ios-dsym-crashlytics.md)**
```

`docs/ios-dsym-crashlytics.md` does not exist anywhere in the repository —
this isn't a moved file (no similarly-named file exists elsewhere either).
This is simultaneously a broken link (priority 1) and a coverage gap: a doc
referenced from another doc that was never created (coverage duty #2).
Proposed action for a future active run: either write the missing guide (the
"Quick Reference" `mdfind`/`dwarfdump` snippet already in `iosApp/README.md`
could seed it) or remove the link and fold the one paragraph of context
directly into `iosApp/README.md`.

---

## Coverage gaps (module directories with substantial code, no describing doc)

Directories with 10+ `.kt` files and no top-level `.md` doc (coverage duty:
"a module or top-level directory with substantial code... and no README or
doc describing it"):

| Directory | `.kt` file count | Notes |
|---|---|---|
| `feature/trip-planner` | 273 | Large surface, but its `ui/` submodule already has 5 dedicated docs (`SEARCH_STOP_UX.md`, `ADDRESS_SEARCH_ELIGIBILITY.md`, `docs/fuzzy_stop_search.md`, etc.) — lower-priority gap, an umbrella doc would mostly duplicate those. |
| `feature/track` | 32 | No doc at any level. |
| `feature/departures` | 27 | No doc at any level. |
| `feature/park-ride` | 16 | No doc at any level. |
| `discover` (+ `discover/network`) | 16 + 11 | No doc at any level. |
| `feature/debug-settings` | 14 | New module (shipped as part of the BFF integration work, see Finding §3); no doc describing its 3-submodule split or the `NetworkSource` selector it implements. |
| `core/remote-config` | 10 | At the threshold; lower confidence this is a real gap given its small, single-purpose surface. |

Per the charter, `report-only` mode lists gaps here rather than creating docs.
Proposed priority for an active run, per coverage duty guidance (priority 5,
under-60-line factual docs only): `feature/debug-settings` first (newest,
least likely to be tribal knowledge already), then `feature/track` /
`feature/departures` / `feature/park-ride` / `discover`.

---

## Deferred items

None of the above required deferring due to the 500-line budget — this run
made no file changes, so the budget was not a constraint. All findings above
are deferred to a future `active`-mode run (or human action), since
`report-only` makes no doc changes by design.

## Proposed action queue for the next `active` run, in charter priority order

1. Fix broken links: `TESTING.md` (Finding §2), `iosApp/README.md` (Finding §9).
2. Archive with tombstones: `docs/bff-integration-plan.md` (§3),
   `docs/plans/STOP_LABEL_ANALYTICS_PLAN.md` (§6),
   `feature/trip-planner/ui/STOP_LABEL_UX_REDESIGN_PROPOSAL.md` (§8).
3. Update index/README files to match the above archive moves (none currently
   link to the three plans above by relative path other than their own
   siblings — verify at archive time).
4. Trim the stale `distribute-google-play-manual.yml` line from
   `docs/ci_cd/ci-cd-architecture.md` (§4).
5. Create a small doc for `feature/debug-settings` (coverage gap, highest
   priority of the gaps listed above).
