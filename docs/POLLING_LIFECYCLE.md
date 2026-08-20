# Polling & WhileSubscribed Lifecycle Rules

All polling flows use `SharingStarted.WhileSubscribed(threshold)` so they stop when no UI
is collecting them. This only works if the UI collects with lifecycle awareness.

## Rule: use `repeatOnLifecycle(STARTED)` to activate side-effect flows

`LaunchedEffect` is Composition-scoped — it keeps subscribers alive through background and
lock-screen, defeating `WhileSubscribed` and causing continuous API calls when the user is
not looking at the screen.

**Correct pattern:**

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
LaunchedEffect(viewModel, lifecycleOwner) {
    lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { viewModel.pollingFlow.collect {} }
    }
}
```

`repeatOnLifecycle(STARTED)` cancels inner coroutines when the Activity goes to background
(STOPPED), dropping subscriber count to 0 so `WhileSubscribed` halts the poll loop. It
restarts automatically on foreground.

**Wrong — keeps polling through lock-screen:**

```kotlin
// ❌ LaunchedEffect is Composition-scoped, not Activity-lifecycle-scoped
LaunchedEffect(viewModel) {
    launch { viewModel.pollingFlow.collect {} }
}
```

## Rule: collect `uiState` with `collectAsStateWithLifecycle()`

`collectAsState()` is not lifecycle-aware. Always use `collectAsStateWithLifecycle()` for
any `StateFlow` that a `WhileSubscribed` poll depends on.

## How it fits together

```
UI subscribes via collectAsStateWithLifecycle() or repeatOnLifecycle(STARTED)
  → Activity goes to background → subscriber count drops to 0
  → WhileSubscribed(threshold) fires after threshold ms
  → onStart coroutine (while-true poll loop) is cancelled
  → no more API calls
```

## The register

Every `SharingStarted.WhileSubscribed(` in production source appears in one of the two tables
below, and `PollingLifecycleGuardTest` (in `:composeApp` androidHostTest) fails if one does not.
Adding a flow is therefore a deliberate choice between the two: does it do repeating work while
subscribed, or is it just state?

### Polling flows — the UI MUST activate these with `repeatOnLifecycle(STARTED)`

These run a loop or a network call for as long as they have a subscriber. Collecting one from a
plain `LaunchedEffect` keeps it running behind the lock screen.

| Flow | What it does |
|---|---|
| `TimeTableViewModel.autoRefreshTimeTable` | 30s trip refresh |
| `TimeTableViewModel.isLoading` | triggers `fetchTrip()` on screen entry |
| `TimeTableViewModel.isActive` | 10s time-text refresh |
| `TrackTripViewModel.uiState` | GTFS-RT live tracking poll |
| `TripPoller.liveOverlay` | GTFS-RT overlay for the tracked trip |
| `TripPoller.stopCoordinates` | stop coordinates for the tracked trip |
| `TripPoller.countdownDisplay` | 1s countdown tick |
| `DeparturesViewModel.isActive` | 10s relative-time-text refresh, activated by `DeparturesRelativeTimeTicker` |
| `DeparturesViewModel.init` | departure board poll, gated on `_uiState.subscriptionCount` |
| `SavedTripsViewModel.pollExpandedParkRideStops` | Park & Ride availability for cards the rider has open; gated on `uiState` subscribers AND on at least one card being open |
| `SplashViewModel.isLoading` | app-start work triggered `onStart` |

### State-only flows — `collectAsStateWithLifecycle()` is enough

`WhileSubscribed` here only releases upstream collectors; nothing repeats, so there is no
background-work hazard. They are listed so that a new flow cannot land unclassified.

`SavedTripsViewModel.whileScreenSubscribed` is a `SharingStarted` strategy rather than a flow:
it is `WhileSubscribed` with the screen's real subscriber count mirrored out on its way past, so
the review moment can tell whether the rider is still on Saved Trips. It starts no work of its
own, so it is classified here with the state it shares.

| Flow |
|---|
| `AddParkRideViewModel.uiState` |
| `DateTimeSelectorViewModel.uiState` |
| `DebugSettingsViewModel.state` |
| `DiscoverViewModel.uiState` |
| `IntroViewModel.uiState` |
| `MapStopSelectionViewModel.mapUiState` |
| `SavedTripsViewModel.uiState` |
| `SavedTripsViewModel.whileScreenSubscribed` |
| `SearchStopViewModel.uiState` |
| `ServiceAlertsViewModel.uiState` |
| `SettingsViewModel.uiState` |
| `ThemeSelectionViewModel.uiState` |

Moving a flow between the tables is the whole point of the register: if a state-only flow grows
an `onStart` loop, it belongs in the first table and its screen needs `repeatOnLifecycle`.

## Rule: one screen, one `WhileSubscribed` grace value

`TimeTableViewModel`'s three flows above all answer the same question — is the timetable on
screen? — so they share `SCREEN_VISIBILITY_GRACE` (5 s). They previously carried two different
values (5 s and 3 s), which left a two-second window where the screen was half alive: the
refresh loop had stopped while the entry hook was still armed.

Five seconds is the standing figure here: long enough that a configuration change re-subscribes
well inside it, short enough that leaving the screen stops network work promptly. If you add a
flow to a screen that already has one, use that screen's constant rather than a fresh literal.

## Rule: a pinned timetable is not a live board

Auto-refresh exists to keep *live* data fresh. A timetable the rider pinned to a moment that
has not arrived yet is a plan, and re-fetching it changes nothing they can see.

`shouldAutoRefresh(selection, now)` (in `timetable/TimeTableRefreshPolicy.kt`) decides this, and
it compares **instants**, not calendar dates. Comparing dates — which is what the old
`LocalDate.isFuture()` check did — treats "Leave at 6pm this evening" as not-future, so the
screen refreshed a plan every 30 seconds for the rest of the day. Both `LEAVE` and `ARRIVE`
resolve to the one moment the rider named; a selection at or before now is a live board again.

## Rule: a refresh interval is a budget per subject, not per collector

`WhileSubscribed` stops polling when nobody is looking. It says nothing about what happens
when **two** collectors look at the same thing at once, and that is a separate defect: two
surfaces polling one stop, each running its own loop, doubles the API calls while both
appear to be respecting the interval.

`DepartureBoardRepository.pollStop` is the worked example. Every collector still gets its
own `channelFlow` — that is what keeps cancellation simple — but the network call is gated
by `fetchIfWindowOpen`, which claims the stop's refresh window under a mutex **before**
issuing the call. The first session in fetches; the others skip and read the result from the
shared cache entry. Writing the claim before the call rather than after it is the whole
mechanism: two sessions waking in the same instant would otherwise both see a stale
timestamp and both fetch.

If you add a poll loop that shares a subject with another loop, gate it the same way, and
test it the same way: two concurrent collectors, one pumped interval, assert the call count
is one. A test with a single collector cannot see this class of bug.

## Testing polling: the clock must be the scheduler's

A refresh window compares a timestamp against wall-clock time while the loop advances on
`delay`. In a test those are different clocks unless you make them one — virtual time jumps
30 seconds while `Clock.System.now()` barely moves, so the window never opens and the
assertion passes or fails for the wrong reason. Inject `KrailTestScope.clock` (or
`virtualClock(scheduler)` where the test still owns a bare `StandardTestDispatcher`) into
the `clock` seam of anything under test that has one.
