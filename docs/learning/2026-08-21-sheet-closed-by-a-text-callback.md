# A bottom sheet that "the dialog dismissed itself" was closed by a text field

**2026-08-21** · Search-stop map, journey map, map options · ~4 builds, 3 install cycles, one evening

## Symptom

Open a stop's details sheet on the search-stop map, switch the device to dark mode (or change
font size), and the sheet is gone. The screen behind it is gone too: the map has been replaced
by the recents list, as if the rider had never tapped "Select on map".

## Root cause

Three separate faults, none of them the one that had been written down.

1. `SearchStopMap` and `JourneyMap` held which stop's sheet is open in a plain `remember`, so
   Activity recreation dropped it. `MapOptionsBottomSheet` did the same with the rider's
   unsaved radius/mode/toggle edits, on a sheet that itself stays open — so the edits reverted
   under an unchanged-looking sheet.
2. `SearchStopScreen` switched from map back to list on any text-field callback. The field
   re-emits its restored value when the Activity is recreated, and that read as typing. The
   comment right above it already recorded the same trap for focus callbacks; the value
   callback had the same problem and was not covered.
3. Phone portrait and landscape are different layouts (single pane vs dual pane) and use two
   different map composables, so no `rememberSaveable` inside either can carry state across a
   rotation. That one is still open — see the gap below.

## Why it took so long

The starting theory, written into the issue and into `TimeTableStopSheetRestoreTest`'s KDoc,
was that `ModalBottomSheet` fires `onDismissRequest` while its dialog is disposed during
recreation, so the caller's dismiss handler clears the state. That is a good story: it explains
the symptom, it matches how M3 hosts a sheet in a real `Dialog`, and it also explains why
`StateRestorationTester` cannot see the bug.

It is not what happens. A probe build that logged every `onDismissRequest` with the lifecycle
state and a stack trace showed:

- rotating with a sheet open logged **nothing** — no dismiss request at all, and the
  date-time sheet (whose visibility flag is already saveable) came back on screen;
- the only `onDismissRequest` in the whole session came from `ModalBottomSheet.kt:157`
  (`animateToDismiss`) at lifecycle `RESUMED` — a scrim tap, not a disposal.

The dismissal theory had blocked the obvious question, which is whether the state was saveable
at all. It was not.

A second trap was self-inflicted: `adb shell input tap` opens a sheet and instantly closes it
again, because the `ACTION_UP` lands on the dialog window that the `ACTION_DOWN` just created,
and M3 treats a touch outside the sheet as a dismiss. That looks exactly like the bug under
investigation. Splitting the tap into `input motionevent DOWN` / `input motionevent UP` leaves
the sheet open.

## What would have caught it sooner

- **Log the callback you are accusing.** One probe build settled a theory that three rounds of
  reading library source could not. `Throwable().stackTraceToString()` inside the suspect
  lambda names the exact library line that called it.
- **Check whether the state can survive before explaining why it does not.** `remember` vs
  `rememberSaveable` is a one-line question, and it outranks any mechanism story.
- **Drive sheets with split motion events.** `adb shell input tap` is not a usable way to open
  a bottom sheet.
- **Read the screen's own recomposition log.** `[SEARCH_STOP_SINGLE_PANE] recomposed: showMap=false`
  after a dark-mode switch was the whole of fault 2, sitting in logcat the entire time.

## Actions taken

- [x] Sheet-visibility state on both maps is now a saveable stop id, re-resolved from the map
      state the ViewModel still holds (the same lookup the map layer does for a tap).
- [x] `MapOptionsBottomSheet`'s pending edits are saveable, pinned by
      `MapOptionsSheetRestoreTest` — verified to fail when the fix is reverted.
- [x] The map/list switch ignores a text callback that carries unchanged text.
- [x] `TimeTableStopSheetRestoreTest`'s KDoc no longer states the dismissal theory as fact.
- [ ] **Open gap:** on a phone, rotating crosses single pane and dual pane, which are two
      different map composables, so an open stop sheet is still lost. Fixing it means hoisting
      the selection above `AdaptiveScreenContent` and giving both panes the same sheet, the way
      `showMap` is already hoisted. Tracked in issue #1915.
