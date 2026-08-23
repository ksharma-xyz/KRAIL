# Stop search opened on the map for four months, and every test agreed it was fine

**2026-08-23** · **Search stop / landing surface** · **Cost: one user report; the diagnosis itself was quick**

## Symptom

Tapping the From or To field on the home screen opened the stop search screen **on the map**,
not on the list of recent stops. The search field was not focused and the keyboard was not up,
so typing a stop name did nothing until the rider tapped the field first. On tablets and in
phone landscape the screen was correct — list left, map right — which is what made it look
like a phone-only rendering fault rather than a flag.

## Root cause

One line in `SearchStopScreen.kt`:

```diff
-    var showMap by rememberSaveable { mutableStateOf(false) }
+    var showMap by rememberSaveable { mutableStateOf(true) }
```

Flipped in `8122380db`, *"feat: add StopLabel model and stop-labels field to state (#1521)"* — a
commit about labels, with no reason to touch the map flag and no mention of it in its message.

Three consequences followed from that one boolean, none of them obvious from the line itself:

1. The `LaunchedEffect(showMap)` immediately below it hides the keyboard and frees field focus
   whenever the map is up. Map-first therefore meant landing on a screen that could not be
   typed into.
2. `MapAutoInitEffect` initialises MapLibre while the map is showing, so every visit to stop
   search built the whole map stack whether or not anyone wanted a map.
3. `SearchTopBar` is passed `isMapAvailable = false` — the map pill was removed and the only
   way into the map is the **"Select on map"** row inside the list. Opening on the map bypassed
   the design's own entrance.

## Why it took so long

**Nothing could see the flag.** The list renders when `!showMap || mapState == null`. Every
Compose test fixture had `mapUiState = null`, so the list rendered either way and all eight
existing tests passed identically with the flag `true` or `false`. The suite was not weak here;
it was blind, and blind in a way that reads as coverage.

**The E2E lane worked around it instead of failing.** `02-plan-trip` called
`shared/dismiss-map-options.yaml` twice, because on a fresh device the first-run map options
sheet opened over the map and covered `searchstop.query`. That workaround was added, correctly,
to fix a red nightly — and it made the wrong landing surface survivable, so the lane went green
against a screen opening in the wrong place. `03-rotation-sweep` had no such call and passed
only because `02` ran earlier in the same invocation and dismissed the sheet: a cross-flow order
dependency nobody had written down.

**Dual-pane hid it from the layouts most likely to be checked.** `SearchStopScreenDualPane`
never reads `showMap`. Tablet, foldable and phone landscape were all correct throughout, so
every check on those layouts was a check that could not fail.

## What would have caught it sooner

- **A default that changes behaviour needs a test in the state where the default is visible.**
  Here that is an *initialised* map. Testing a screen with the expensive dependency stubbed out
  to null is worth doing, but it does not test the branch that chooses between them.
- **A workaround in an E2E flow is evidence about the app, not only about the flow.** The
  `dismiss-map-options` call existed because stop search opened on a map. That fact was written
  in the flow's own header for four months and read as a CI quirk.
- **Ask what a one-line flag flip does to its neighbours.** The keyboard effect and the map
  initialiser both sit within twenty lines of the flag and both changed meaning.

## Actions taken

- [x] `showMap` back to `false`, with a comment naming the keyboard effect, the map initialiser
      and the dual-pane exception, so the next reader sees the blast radius from the line.
- [x] `singlePane_opensOnTheList_evenWhenAMapIsAlreadyInitialised` — with `MapUiState.Ready()`,
      the only state that distinguishes list from map. Mutation-checked: reverting the flag
      fails it.
- [x] `selectOnMap_isOfferedInTheList_whenMapsAreAvailable` and `selectOnMap_asksForTheMap_whenTapped`,
      so the map stays reachable and a future "just always show the list" fix cannot pass.
- [x] `02-plan-trip` and `03-rotation-sweep` assert `searchstop.recents` immediately after
      tapping the field: the landing surface is now pinned end to end, on both routes into the
      screen.
- [x] The two `.maestro/shared/dismiss-map-options.yaml` calls removed from `02-plan-trip` — the sheet is opened
      by the map, which is now opt-in, so they were two dead ten-second waits in the one flow
      whose whole history is timing. The shared flow stays, with a header saying when it is
      needed again.
- [x] The landing-surface rule written into `feature/trip-planner/ui/SEARCH_STOP_UX.md`, which
      had only implied it.
