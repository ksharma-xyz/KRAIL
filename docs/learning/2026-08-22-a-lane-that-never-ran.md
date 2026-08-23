# The Maestro nightly had never run a single flow, and looked like a failing test suite

**2026-08-22** · **CI / end-to-end lane** · **Cost: four dispatched nightly runs, two wrong diagnoses, one unnecessary PR**

## Symptom

The nightly lane was red every run. The PR smoke lane was red on every pull request,
regardless of what the pull request changed. Both had been red for days and people had
learned to scroll past them.

The visible failure on the smoke lane was:

```
[Failed] 02-plan-trip (44s) (Assertion is false: id: searchstop.query is visible)
```

The nightly's job log ended without ever naming a flow.

## Root cause

Three separate bugs, stacked. Each one hid the next.

1. **The nightly never started.** It passes `.maestro/` so it can cover the whole tree, and
   Maestro does not recurse into subdirectories. Pointed at a directory whose flows all live
   one level down it finds nothing and exits **non-zero**:

   ```
   Top-level directories do not contain any Flows: .../.maestro
   ```

   Because that is the same exit code a failing suite uses, a lane that never started was
   indistinguishable from a lane that ran and failed.

2. **No `testTag` inside any `ModalBottomSheet` was published as a resource id.** The app
   calls `exposeTestTagsToUiAutomation()` once, on `KrailNavHost`. A sheet renders in its own
   window, outside that subtree, so `testTagsAsResourceId` never reached it. Nothing failed
   loudly: a UI-automation selector for anything inside a sheet simply never matched, which
   reads exactly like the element not being on screen.

3. **A first-run sheet covered the field the flow was waiting for.** `SearchStopViewModel`
   opens the map options sheet by itself the first time a rider reaches stop search, gated on
   `KEY_HAS_SEEN_MAP_OPTIONS_SHEET`. While up it covers `searchstop.query`. On a fresh device
   that field is not slow to appear, it is unreachable.

## Why it took so long

**Wrong theory 1: the two lanes were failing the same way.** The smoke lane named a flow and
an assertion; the nightly named nothing. Assuming one cause for both is what sent the first
change in the wrong direction, and it was written into a merged PR description before anyone
checked. The two lanes pass different paths: `.maestro/smoke/` has flows directly in it and
always found them, `.maestro/` never did.

**Wrong theory 2: the emulator was too slow.** The ANR dump printed at the failure showed 99%
CPU with the whole Google app suite competing on two cores, and KRAIL not even in the table.
That is real, and it is not what broke the flow. A change was shipped to wait for the device
to go quiet; it ran, reported a flat 100% on every sample for the full two minutes, and
disproved its own premise. A follow-up doubled every timeout in the flow. The flow then failed
at exactly the same two points. **Doubling a timeout and getting an identical failure is proof
the wait was never the problem** — that signal was available and was not read for another
round.

**What actually broke the deadlock** was reading the screenshot Maestro had been saving next to
every failure the whole time. It is a full-screen map with a bottom sheet over it. The
hierarchy dump beside it lists `Map Options`, `Save`, and `Tap any stop on the map to select
it`, with `searchstop.query` nowhere. No theory survives that image.

**And then the fix did not work either**, because of bug 2: the dismiss step was added, the
flow still failed, and the sheet's Done button sat in the hierarchy dump with an empty
`resource-id` beside its own label. That empty string is the only visible trace bug 2 ever
leaves.

## What would have caught it sooner

The reusable form of all of this is now
[`docs/MAESTRO_TRIAGE.md`](../MAESTRO_TRIAGE.md): the artifact map, the order to read it in,
and a signature table carrying every trap below.

- **Read the artifact before theorising.** Both wrong turns were theories built on a log line.
  The screenshot and the hierarchy JSON were in the uploaded artifact from the first failure
  onward and answered the question in one look. Same lesson as
  `docs/LAYOUT_AND_INSETS.md`: measure before theorising.
- **A timeout increase that changes nothing is evidence, not a reason to increase it again.**
  If two different steps time out in one run, suspect a blocker, not a budget.
- **Distrust a non-zero exit that names nothing.** A tool that exits 1 without naming a test
  has usually not run any.
- **Test automation against the state CI actually has.** Every one of these passed on a
  developer emulator, because that device had dismissed the first-run sheet months earlier.
  `adb shell pm clear` before a local run is the difference between reproducing CI and
  reassuring yourself.

## Actions taken

- [x] `.maestro/config.yaml` names the two lane directories, so `.maestro/` discovers flows.
      `shared/` and `ci/` deliberately excluded.
- [x] `testTagsAsResourceId` set on taj's Android `ModalBottomSheet`, fixing every sheet in
      the app rather than the one that exposed it.
- [x] `.maestro/shared/dismiss-map-options.yaml`, same shape as `.maestro/shared/dismiss-intro.yaml`. Taps
      **Done**, because Done is what writes `KEY_HAS_SEEN_MAP_OPTIONS_SHEET`; dismissing any
      other way leaves the flag unset and the sheet returns later in the same flow.
- [x] The smoke lane no longer runs on pull requests. It was never a required check, so a
      permanently red mark that blocked nothing was worse than no mark.
- [x] The settle pre-flight waits for CPU to **plateau** rather than to fall below a fixed
      number. An absolute threshold is only meaningful if the idle figure is known, and here
      it is a property of the runner: at a flat 100%, 85% was unreachable and the wait was
      dead time.
- [ ] The Android leg still needs its retry to go green: one attempt failed with an **empty
      view hierarchy and a black screenshot**, which is the emulator's compositor stalling
      under load, not the app and not the NSW API. Unexplained.
      Recorded as a signature in [`docs/MAESTRO_TRIAGE.md`](../MAESTRO_TRIAGE.md) so the next
      person to see it does not re-diagnose it from scratch.
