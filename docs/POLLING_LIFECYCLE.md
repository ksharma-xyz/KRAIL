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

Polling flows in this codebase that use this pattern:
- `TimeTableViewModel.autoRefreshTimeTable` — 30s trip refresh
- `TimeTableViewModel.isLoading` — triggers `fetchTrip()` on screen entry
- `TimeTableViewModel.isActive` — 10s time-text refresh
- `TrackTripViewModel.uiState` — GTFS-RT live tracking poll
- `DeparturesViewModel` — departure board poll (gated on `_uiState.subscriptionCount`)
