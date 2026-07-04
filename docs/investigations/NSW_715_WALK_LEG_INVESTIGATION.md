# Investigation: "715 / walk / 715 / T1" shown as 3 legs instead of 1 bus

## User report

Trip from stop `214758` (Seven Hills Rd opp Monaro St) to Redfern Station shows:

```
715 bus -> walk -> 715 bus -> T1 train
```

instead of the expected `1 bus (715) -> 1 train (T1)`. Google Maps and the Opal Travel app
both show the same real-world trip as a single 715 bus leg.

## Raw NSW TripPlanner API response

Full response saved at the time of investigation: `/Users/ksharma/notes/home-redfern-investigate.json`
(9 journey options returned for this origin/destination/time).

The journey matching the user's report is journey index 1 in that response (near-identical
patterns also appear at index 3 and 6, for later departures):

| Leg | Route | tripCode | RealtimeTripId | Transportation description |
|---|---|---|---|---|
| 0 | 715 | **77** | 2647807 | "Seven Hills to Rouse Hill Station via Norwest & Kellyville" (outbound) |
| 1 | walk | — | — | `position: IDEST`, 31 m / 60 s, crosses Seven Hills Rd |
| 2 | 715 | **15** | 2648028 | "Rouse Hill Station to Seven Hills via Kellyville & Norwest" (inbound) |
| 3 | T1 | — | — | Seven Hills Station → Central/Redfern |

Leg 0's origin/destination stops (`214758` → `214759`) and leg 2's origin/destination stops
(`2147213` → Seven Hills Station) are both on Seven Hills Rd near Solander Rd, on opposite
sides of the road — the walk leg is literally "cross the street."

## What the KRAIL codebase does today

`TripResponseMapper.kt` (`feature/trip-planner/ui`) maps every raw API `Leg` 1:1 into a UI
`Leg` (`getLegsList()` → `mapNotNull { it.toUiModel() }`). There is **no logic anywhere** that
compares adjacent legs' route number, stop IDs, or trip IDs to decide whether to merge them.
So KRAIL is rendering exactly what the NSW API sent — 4 legs, faithfully.

## Is it really "the same bus"?

This is the crux of the user's complaint, and the honest answer is: **NSW's API data alone
doesn't prove or disprove it, and it doesn't matter for the fix.**

- `tripCode` (77 vs 15) and `RealtimeTripId` (2647807 vs 2648028) differ — these are two
  distinct GTFS trips in NSW's schedule data.
- The transportation `description` fields are opposite directions ("to Rouse Hill" vs "to
  Seven Hills") — i.e. NSW's own model has the passenger travelling one stop the *wrong*
  way, walking across the road, then boarding a bus travelling *back* the way they came.
- The NSW TripPlanner API response does **not** expose a GTFS `block_id` (the field that
  would prove "same physical vehicle, continuing as a new trip after a scheduled reversal").
  Only `tripCode`, `AVMSTripID`, `RealtimeTripId`, `gtfsTripId` are present — none of which
  encode vehicle/block continuity.
- It is plausible this is genuinely the same physical bus reaching a short local reversal
  point near Solander Rd and continuing on its return working — NSW buses commonly do this.
  It is equally plausible it's two different scheduled trips that happen to overlap in time
  at this stop pair. **The API data available to KRAIL cannot distinguish these cases.**

Google Maps and Opal Travel almost certainly aren't proving vehicle continuity either — they
are very likely applying a **display-layer heuristic** (same route number + trivial walk +
short time gap ⇒ show as one bus), not a data-verified "same vehicle" fact. This is the gap:
KRAIL has no such heuristic; those products do.

## Why this journey option exists at all

Journey index 0 in the same response is a strictly better option: direct 715 (no backtrack)
to Norwest Station → M1 Metro → Central, departing ~4 minutes later than journey 1's first
leg and **arriving at the exact same time** (`00:35:06`). NSW's engine returns both options
without deduplicating/deprioritizing the worse one. That's a secondary, separate issue from
the display problem below.

## Recommendation: add a display-layer merge heuristic

Don't try to prove "same vehicle" — match what Google Maps/Opal visibly do: merge two
transit legs of the **same route number** separated by a **trivially short walk** into one
displayed leg, regardless of whether the underlying trip IDs match.

### Proposed heuristic (candidate for `TripResponseMapper.kt`)

Add a pre-processing pass over `journey.legs` **before** `getLegsList()` runs, that collapses
a 3-leg window `[TransportLeg A, WalkLeg, TransportLeg B]` into a single displayed
`TransportLeg` when **all** of the following hold:

1. `A.transportation.number == B.transportation.number` (same route number, e.g. "715") —
   this is the strongest and simplest signal, and it's what a passenger visually sees at the
   stop (same route number bus pulls up).
2. The walk leg's `footPathInfo.position == "IDEST"` (whole leg is walking, not a
   partial-leg annotation) — confirms it's a genuine standalone walk, not noise.
3. The walk leg is short: e.g. `duration <= 120` seconds (2 min) — tune against real
   fixtures; NSW's own "trivial interchange" cases (`Leg.interchange` field, `type: 100`)
   suggest ~1-2 min is the right order of magnitude for "not a real walking leg."
4. Do **not** require direction/description to match — direction is exactly the case that
   differs in this report, and the merge should still apply since that's what users expect
   to see collapsed.

When merged, render:
- `origin` = leg A's origin, `destination` = leg B's destination
- `departureTime`/`arrivalTime` spanning the whole window (so travel time / totalWalkTime
  stays accurate underneath)
- Optionally, a small annotation on the card (e.g. "change side of street") reusing the
  existing `WalkInterchange`/`WalkPosition` model in `TimeTableState.kt`, rather than
  inventing a new UI element — this already exists for the "short interchange, not a
  standalone walk" case and is conceptually the same thing.

### What NOT to do

- Don't try to detect "same vehicle" via `tripCode`/`RealtimeTripId` equality — in this real
  case they legitimately differ, so that check would never fire and wouldn't fix the
  complaint.
- Don't merge legs of *different* route numbers (e.g. 715 → 718) even if the walk is short —
  that's a genuine route change a user should see.
- Don't touch the underlying raw `TripResponse.Journey` data used for map visualization
  (`rawDataMap` in `buildJourneyListWithRawData`) — only the UI `Leg` list passed to the
  journey card should be affected, so the map view / detailed stop-by-stop view can still
  show the real walk if the user drills in.

### Where to implement

- New function e.g. `List<TripResponse.Leg>.collapseSameRouteQuickWalks()` in
  `TripResponseMapper.kt`, called from `buildJourneyListWithRawData()` right after
  `journey.getFilteredValidLegs()` (line ~43) and before `legs.getLegsList()` (line ~49).
- Needs test coverage in `TripResponseMapperTest.kt` — at minimum: (a) the exact case from
  this investigation (715/walk/715/T1 → collapses to 715/T1), (b) a same-route-number but
  *long* walk that should NOT collapse, (c) a different-route-number pair that should NOT
  collapse, (d) confirm total travel time / totalWalkTime stay correct across the merge.

## Implementation (this branch)

The merge heuristic above has been implemented on `feature/merge-same-route-quick-walk`,
**not yet reviewed or pushed**.

- `TripResponseMapper.kt`: `getFilteredValidLegs()` now runs `legs?.collapseSameRouteQuickWalks()`
  before the existing `filter { it.transportation != null }`.
- `collapseSameRouteQuickWalks()`: slides a 3-leg window over the raw leg list and, when
  `canCollapseAcross()` matches, replaces `[A, walk, B]` with a single synthetic
  `TripResponse.Leg` via `collapseAcrossQuickWalk()`.
- `canCollapseAcross()` matches when: the middle leg `isWalkingLeg()`, its
  `footPathInfo[0].position == "IDEST"` (genuine standalone walk, not a before/after
  annotation), its `duration <= QUICK_WALK_MAX_SECONDS` (120s), and
  `A.transportation.number == B.transportation.number` (same route number). Trip ID /
  direction are deliberately not compared, per "What NOT to do" above.
- `collapseAcrossQuickWalk()`: keeps leg A's `origin`/`transportation` (route number, mode,
  displayText, tripId all come from A), takes leg B's `destination`, concatenates
  `stopSequence` from both (dropping the walk's own 2 stops), combines `infos` (service
  alerts), and sets `duration = null` so `resolveDurationSeconds()` recomputes the real
  door-to-door time from A's origin timestamp to B's destination timestamp (rather than
  summing durations, which would double count or miss the walk gap).
- Only affects the `legs` variable feeding `getLegsList()`/`getTransportModeLines()`/
  `getTotalStops()`/`totalWalkingDuration` in `buildJourneyListWithRawData()`. The raw
  `rawDataMap` used for map visualization is untouched (map still gets the true 4-leg
  journey from `journey.legs`), and `getFirstPublicTransportLeg()`/`getLastPublicTransportLeg()`
  (used for origin/arrival time) also read `journey.legs` directly, so they're unaffected too.

### Fixtures and tests added

- `feature/trip-planner/network/src/commonTest/assets/trip_seven_hills_redfern_715_backtrack.json` —
  the real captured API response for this investigation (journey index 1, the one that
  reproduces the bug), with only `coords`/`pathDescriptions` arrays stripped (they're
  irrelevant to leg mapping and add ~8000 lines of lat/lng noise). Committed for provenance —
  unlike `OranParkToSevenHillsFixture`'s KDoc, which references a
  `trip_oranpark_sevenhills.json` asset that was never actually committed (checked: it doesn't
  exist in the repo). Don't repeat that — if you trim a fixture from a real response, commit
  the full-fidelity source alongside it.
- `Redfern715BacktrackFixture.kt` (`.../fixtures/`) — the same journey, trimmed further to
  essential fields only (times, IDs, route numbers, `tripCode`/`RealtimeTripId`, `footPathInfo`,
  `interchange`), matching the values in the asset above exactly (T1's 19 intermediate stations
  trimmed to origin+destination; every other leg's `stopSequence` is verbatim). Its KDoc
  explains the full backtrack scenario inline so a reader doesn't need this doc open to
  understand the test.
- Three new tests in `TripResponseMapperTest.kt`, under `//region collapseSameRouteQuickWalks`:
  1. `buildJourneyList collapses same-route legs separated by a quick walk` — decodes
     `Redfern715BacktrackFixture.JSON` (real data, not hand-built). Asserts 2 legs (not 4),
     merged leg's `transportModeLine.lineName == "715"`, `totalDuration == "6 mins"` (the real
     door-to-door window, `00:03:00` → `00:09:42` estimated — not leg A's own 60s), combined
     `stops.size == 10` (2 + 8 real stops), and `totalWalkTime == null`.
  2. `... does not collapse ... when the walk exceeds the quick-walk threshold` — **synthetic**
     (the real capture doesn't contain this variant): a 150s walk (over the 120s cutoff) stays
     as 4 separate legs, and `totalWalkTime` is still populated.
  3. `... does not collapse legs on different route numbers ...` — **synthetic**: 715 → 718
     stays 4 legs even with a trivial walk between them.

  Tests 2 and 3 use two synthetic-fixture builders (`buildRouteLeg`, `buildQuickWalkLeg`) kept
  in the test file itself, clearly labelled as synthetic in their own KDoc, since they pin
  heuristic boundaries the real capture doesn't happen to exercise.

**Bug found and fixed while wiring up the real fixture**: the test class's `json` parser was
missing `isLenient = true`, despite its comment claiming to "mirror how Ktor deserialises
API responses in production" — production (`core/network/HttpClient.kt`,
`core/remote-config/JsonConfig.kt`) does set it. Real NSW payloads send some `String?` fields
(e.g. `Transportation.properties.tripCode`) as unquoted JSON numbers, which strict decoding
rejects. This never surfaced before because `OranParkToSevenHillsFixture` doesn't include
`tripCode`. Fixed by adding `isLenient = true` with a comment explaining why — this benefits
every future real-JSON fixture test in this file, not just this one.

All tests pass, plus the full existing `TripResponseMapperTest` suite (verified via
`./gradlew :feature:trip-planner:ui:testAndroidHostTest`) and
`./gradlew :feature:trip-planner:ui:detekt` / `:feature:trip-planner:network:detekt` (clean
after fixing two findings in the mapper: a doc comment misplaced over a private property, and
`canCollapseAcross` exceeding the max-return-count rule — rewritten as a single boolean
expression).

### How this is meant to scale (kept discoverable on purpose)

- `collapseSameRouteQuickWalks()`'s KDoc in `TripResponseMapper.kt` points back to this file
  and to the test region, and explicitly says "read the 'What NOT to do' section before
  changing this" — so the reasoning travels with the code, not just in a doc that can go stale
  unnoticed.
- This doc is now listed in `CLAUDE.md`'s "Per-feature UX rule docs" section, the same
  mechanism already used for `SEARCH_STOP_UX.md` / `TABLET_FOLDABLE_UX.md` /
  `POLLING_LIFECYCLE.md` — anyone (human or Claude) working on `TripResponseMapper.kt` or
  `TripResponseLegMapper.kt` is directed here before making changes.
- The regression test decodes a real captured response rather than a hand-built object tree,
  so it also catches NSW API shape drift (renamed/retyped fields), not just logic regressions
  in the collapse heuristic itself.

### Not done in this branch (left for follow-up)

- No "change side of street" annotation on the merged card — the doc above suggested reusing
  `WalkInterchange`/`WalkPosition`, but that's a UI-layer change (`TripResponseLegMapper.kt` /
  `JourneyCard.kt`) beyond the mapper fix and needs its own review.
- The window-scan is single-pass and won't chain a second consecutive quick-walk merge
  (e.g. 715/walk/715/walk/715) in one call — no evidence this occurs in practice, not
  handled to avoid speculative complexity.
- The secondary "journey de-duplication" recommendation below is not implemented.

## Secondary recommendation: journey de-duplication

Independent of the display merge above: NSW returns near-duplicate/dominated journeys (see
"why this journey option exists at all"). A journey-quality filter that deprioritizes options
with a same-route backtrack-and-walk pattern when a strictly-dominant alternative exists in
the same response (same or later departure, same or earlier arrival, fewer real interchanges)
would prevent this journey from surfacing prominently at all, on top of fixing its display.
Lower priority than the merge heuristic above, since it changes journey *ranking*, not just
per-journey rendering, and needs its own product discussion (which journey wins when times
aren't identical, how many alternates to suppress, etc).
