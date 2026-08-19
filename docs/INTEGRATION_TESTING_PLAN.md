# Integration testing plan

Status: first milestone built. `FlowTest` lives in `:feature:trip-planner:ui`'s
`androidHostTest` source set and runs in the ordinary `testAndroidHostTest` lane. The
remaining targets below are still proposals.

## Why this exists

A shipped bug motivated it. The Ask KRAIL dialog hands its resolved stops to the home row
through a `LaunchedEffect` in `SavedTripsEntry` keyed on the ViewModel's RESOLVED phase.
That effect re-launches every time the entry recomposes — including coming back from the
stop-search screen — and the state kept the phase after the handoff. Result: the AI's stops
replayed over stops the rider had since picked by hand.

Every existing test layer looked at this bug and shrugged:

- ViewModel unit tests assert emissions. They never re-run the collector, so a collector
  that misbehaves on its second launch is invisible.
- Compose component tests (`AiInputContentLayoutTest`, `AskKrailStatusLineTest`) exercise
  one composable with a handed-in state. They never navigate.
- Snapshot tests capture pixels of static states.
- Nothing tested a **navigation entry**, and nothing tested a **flow across screens**.

The class of bug that lives in the seams — state observed in one place, written in another,
across a navigation boundary or a recomposition lifecycle — reached the rider.

## What exists today (so we do not rebuild it)

| Layer | Where | What it covers |
|---|---|---|
| ViewModel unit tests | `commonTest` (e.g. `AiSearchInputViewModelTest`) | State machine emissions, one collector, virtual time |
| Compose interaction tests | `androidHostTest` + Robolectric (e.g. `SearchStopScreenInteractionTest`) | One screen, real composition, fake data |
| Layout invariant tests | `androidHostTest` (e.g. `AiInputContentLayoutTest`) | Anchoring/child-order rules |
| Snapshot tests | `androidHostTest` + Roborazzi, `@ScreenshotTest` previews | Static visual states |
| Serialization guard | `NavKeySerializationConfigTest` | Every route registered for back-stack restore |
| **Flow tests** | `androidHostTest` + Robolectric, `flow/FlowTest` | A real navigation entry, real ViewModels, leave-and-return and recreation |

All host-side. There are no instrumented `androidTest` targets, no emulator jobs in CI, and
iOS is untested by policy.

## The gap, named

**Flow tests: real ViewModels + real composition + navigation-shaped lifecycle, fake edges.**

A harness that can:

1. Compose a navigation entry (or a small nav graph), not just a screen. **Built** — the real
   entry, see below.
2. Drive it through the lifecycle events that break things: leave and return (dispose and
   recompose the entry), Activity recreation (`StateRestorationTester`), process-death
   restore of the back stack. **Built for the first two**; process death is deferred, see
   "Deferred".
3. Wire real ViewModels with fake services at the module edges (the existing
   `testfakes/` and `core/testing` fakes are most of this already). **Built** — `HomeFlowFakes`
   adds nothing new, it only assembles what was already there.
4. Assert on what the rider sees (semantics tree), not on internals. **Built** — every
   assertion in a flow test goes through `onNodeWith*`.

## The FlowTest API

`feature/trip-planner/ui/src/androidHostTest/.../flow/`

| Piece | What it is |
|---|---|
| `FlowTest` | Base class. Carries the Robolectric annotations, the compose rule, Koin start/stop, and the helpers below. Subclass it and write `@Test`s. |
| `HomeFlowFakes` | Every collaborator the home entry reaches for, faked at the module edge. Builds the three real ViewModels and the Koin module that hands them out. |
| `RecordingTripPlannerNavigator` | The navigation edge. Records which stop-search field the entry asked for; composes no second destination. |

Helpers on `FlowTest`:

| Call | What it does |
|---|---|
| `launchHome()` | Composes the real `SavedTripsEntry` and waits for the first load plus the row's slide-in. |
| `leaveAndReturn()` | Disposes the entry and composes it again. Every `LaunchedEffect` in it is cancelled and re-launched; ViewModels are not. This is going to the stop-search screen and coming back. |
| `recreate()` | `StateRestorationTester` round trip: the composition is saved, discarded and restored from its `rememberSaveable` values. ViewModels survive, matching a real configuration change. |
| `letTimePass(millis)` | Advances the compose frame clock **and** the Robolectric main looper. Both are needed, see "Two clocks". |
| `askKrail(sentence)` | Opens the Ask KRAIL surface, fills it and sends it, one composition apart. |
| `pickStop(result)` | Plays the stop-search screen's part by putting a `StopSelectedResult` on the real `ResultEventBus`, which is exactly what `SearchStopScreen` does before navigating back. |

```kotlin
class MyFlowTest : FlowTest() {
    @Test
    fun theRowKeepsTheRidersOwnPick() {
        fakes.aiTextService.extractionResult = TripIntentExtraction(
            originText = "Central Station",
            destinationText = "Town Hall",
        )
        launchHome()

        askKrail("central to town hall")
        letTimePass(SETTLE_BEAT_MILLIS)          // the surface closes itself
        pickStop(
            StopSelectedResult(
                fieldType = SearchStopFieldType.FROM,
                stopId = "10103",
                stopName = "Parramatta Station",
            ),
        )

        leaveAndReturn()                          // the effects re-launch here

        composeRule.onNodeWithText("Central Station").assertDoesNotExist()
        composeRule.onNodeWithText("Parramatta Station").assertIsDisplayed()
    }
}
```

## Robolectric and Navigation 3: what composed and what did not

### Composed cleanly, no work needed

- **The real entry.** `entryProvider<NavKey> { SavedTripsEntry(navigator) }` is the same
  builder `collectEntryProviders` uses in `:composeApp`, and `NavEntry.Content()` is the same
  call `NavDisplay` makes for the top of the back stack. Both work under Robolectric with the
  plain `createComposeRule()`, so the harness needs no navigation scaffolding at all.
- **Koin.** `startKoin { modules(...) }` in `@Before` is enough for the entry's
  `koinViewModel()` calls, including the keyed one and the one taking `parametersOf`.
- **`rememberUserLocationManager()`** — the aagya permission controller and dhruva location
  tracker both build against Robolectric's `ComponentActivity`. This was the piece expected to
  fight back and it did not. Nothing asks for a permission unless the location lambda is
  called, which the harness never does.
- **`rememberAdaptiveLayoutInfo()`**, `blur()` under `GraphicsMode.NATIVE`, the Ask KRAIL
  surface's real `Dialog`, and the real singleton `ResultEventBus`.

### Cut, and why

- **`NavDisplay` and its decorators.** Rendering `Content()` directly skips
  `SaveableStateHolderNavEntryDecorator` and the per-entry ViewModel store, so ViewModels are
  scoped to the Activity rather than to the nav entry, and `rememberSaveable` inside the entry
  is held by the Activity-level registry. Both survive `leaveAndReturn()` and `recreate()`,
  which is the property the flow tests depend on, so the substitution is faithful for this
  class of test. It would not be faithful for a test about a ViewModel being *cleared* when its
  entry pops off the back stack; that needs the decorators, and therefore `NavDisplay`.
- **The right pane.** `MapStopSelectionPane` (MapLibre) never composes because the Pixel6
  portrait qualifier is single-pane. `MapStopSelectionViewModel` is still built, so its Koin
  binding is still required. A dual-pane flow test would have to deal with MapLibre under
  Robolectric, which is untried.
- **The GPS lambda.** The entry passes `suspend { userLocationManager.getCurrentLocation() }`
  through `parametersOf`; the harness's Koin definitions ignore params and pass
  `resolveCurrentLocation = { null }`. A flow that needs a GPS-derived origin has to fake
  `NearbyStopsRepository` instead.
- **Typing into the Ask KRAIL field.** `askKrail` sends the surface's own events rather than
  driving its text field through semantics. Assertions still go through the semantics tree;
  only the input is shortcut.

### Two clocks

Compose animations run on the test frame clock (`composeRule.mainClock`). A `delay()` inside a
ViewModel runs on the Robolectric main looper's queue. Neither advances the other, and a flow
test usually needs both: the settle beat that closes the Ask KRAIL surface is a `delay`, and
the row it closes onto is revealed by an animation. `letTimePass` advances both, and
`launchHome`, `leaveAndReturn` and `recreate` all end with one.

### A node can exist before it is displayed

The bottom search row is held off screen until the saved-trips load has emitted once, then
slides in. `assertIsDisplayed()` straight after composition fails with *"...is not displayed"*
rather than *"could not find any node"*, which reads like a missing node and is not one. The
harness settles the reveal so tests do not have to know this.

### The surface answers back

`TextField` (taj) reports its contents from a `LaunchedEffect` keyed on them, so composing the
Ask KRAIL surface echoes a `TypedTextChanged` for the empty field and another when the field
syncs to state. Sent as one batch, `OpenInput` + `TypedTextChanged` + `Submit` let those echoes
land *during* the resolve, and `TypedTextChanged` steps the phase back to IDLE. The Ask KRAIL
flow test passed under both the fixed and the pre-fix ViewModel until each event was composed
through before the next one was sent.

**This is the trap to remember about flow tests in general:** driving a real surface's
ViewModel faster than the surface can compose produces a state no rider can reach, and a test
standing on it proves nothing. The check that catches it is the one below.

### Prove the test discriminates

A flow test that cannot fail is worse than no test, because it reads as coverage. Before
committing one, revert the fix it pins (`git revert --no-commit <fix>`), watch it fail, read
the failure message to confirm it fails *for the reason you think*, and restore. For the Ask
KRAIL replay test the pre-fix run fails with the AI's origin back on the row:

```
Failed: assertDoesNotExist.
Reason: Did not expect any node but found '1' node that satisfies:
  (Text + InputText + EditableText contains 'Central Station')
Node found: Node #108 ... Role = 'Button'  Text = '[Central Station]'
```

## Remaining targets, in value order

1. ~~**Ask KRAIL handoff replay**~~ — built: `AskKrailHandoffFlowTest`.
2. ~~**Search-stop round trip**~~ — built: `SearchStopRoundTripFlowTest`. Unlike the replay
   test there is no shipped defect to fail against, so what stands in for that proof is a
   positive control on the helper: a `DisposableEffect` probe inside the harness's entry shows
   `recreate()` really disposing and recomposing it, rather than the test passing because
   nothing happened. Run that probe again if `recreate()` is ever reworked.
3. **Time chip journey** — AI-resolved time shows on the row, travels to the timetable route,
   survives process death (it rides `TimeTableRoute` for exactly this reason).
4. **Dialog-open recreation** — rotate with the Ask KRAIL surface open at each phase (idle,
   listening stopped, unresolved-with-banner) and assert the phase-appropriate content.

## Deferred

- **`ActivityScenario.recreate()` for a full Bundle round trip.** `createComposeRule()` owns
  its Activity and does not expose the scenario, and switching to
  `createAndroidComposeRule<ComponentActivity>()` does not help on its own: content set through
  the rule belongs to the original Activity instance, so a real recreate leaves nothing
  composed. Doing it properly needs a test Activity that calls `setContent` in `onCreate`, and
  therefore a manifest, which every test here avoids with `manifest = Config.NONE`.
  `StateRestorationTester` already covers the part that breaks in practice (saveable state
  restoring into a fresh composition), and the back-stack serialisation a Bundle round trip
  would add is covered by reflection in `NavKeySerializationConfigTest`. Worth revisiting only
  if a defect escapes both.
- **Instrumented `androidTest` on an emulator in CI** only if the host-side harness cannot
  represent window-level behaviour we care about (IME insets, dialog windows). Costly: new CI
  lane, flake budget, device images. Nothing so far has needed it.
- **Maestro or similar UI-driver flows** for a tiny smoke set on release candidates.
  Explicitly out of scope for per-PR CI.

## Ground rules

- Fakes at the edges, real everything else. No mocking frameworks (repo convention).
- Assert through semantics (what is announced/shown), not through internal state, so the
  tests survive refactors. Driving a ViewModel with the events its own surface sends is
  allowed; reading state off it to assert is not.
- Every flow test documents which shipped defect or checklist item it pins.
- Every flow test is proved to fail against the defect it pins before it is committed.
- Keep the suite in `testAndroidHostTest`; a flow test slow enough to need its own lane is a
  design smell in the harness.
- When a flow test catches something a unit test could have pinned, add the unit test too and
  keep the flow test for the seam.

## Definition of done for the first milestone

- [x] The Ask KRAIL handoff replay test exists, fails on the pre-fix commit, passes on the fix.
- [x] A `FlowTest` base exists with recompose-entry and recreation helpers, documented in this
      file.
- [x] `docs/FEATURE_QUALITY_CHECKLIST.md`'s section on re-launched effects points here.
