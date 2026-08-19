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
| `DeparturesViewModel.isActive` | 10s relative-time-text refresh |
| `DeparturesViewModel.init` | departure board poll, gated on `_uiState.subscriptionCount` |
| `SplashViewModel.isLoading` | app-start work triggered `onStart` |

### State-only flows — `collectAsStateWithLifecycle()` is enough

`WhileSubscribed` here only releases upstream collectors; nothing repeats, so there is no
background-work hazard. They are listed so that a new flow cannot land unclassified.

| Flow |
|---|
| `AddParkRideViewModel.uiState` |
| `DateTimeSelectorViewModel.uiState` |
| `DebugSettingsViewModel.state` |
| `DiscoverViewModel.uiState` |
| `IntroViewModel.uiState` |
| `MapStopSelectionViewModel.mapUiState` |
| `SavedTripsViewModel.uiState` |
| `SearchStopViewModel.uiState` |
| `ServiceAlertsViewModel.uiState` |
| `SettingsViewModel.uiState` |
| `ThemeSelectionViewModel.uiState` |

Moving a flow between the tables is the whole point of the register: if a state-only flow grows
an `onStart` loop, it belongs in the first table and its screen needs `repeatOnLifecycle`.
