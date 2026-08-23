# Triaging a Maestro failure

The nightly is red, or the smoke lane is. This is what to open, in what order, and what the
answer looks like when you find it.

The one rule that matters, learned the expensive way in
[`docs/learning/2026-08-22-a-lane-that-never-ran.md`](learning/2026-08-22-a-lane-that-never-ran.md):
**read the artifact before theorising.** Both wrong turns in that incident were theories built
on a single log line, while the screenshot and hierarchy dump that answered the question had
been in the uploaded artifact since the first failure. Same lesson as
[`LAYOUT_AND_INSETS.md`](LAYOUT_AND_INSETS.md): measure before theorising.

---

## Where the evidence is

Both legs upload one artifact per run, pass or fail, from
[`.github/workflows/maestro-nightly.yml`](../.github/workflows/maestro-nightly.yml):

| Leg | Artifact | Contents |
|---|---|---|
| Android | `maestro-nightly-android-<run_id>` | `maestro-output/` + `logcat-android.txt` |
| iOS | `maestro-nightly-ios-<run_id>` | `maestro-output/` only |

```
maestro-output/tests/<timestamp>/
  maestro.log                                  run-level driver log
  <flow-name>/
    commands.json                              every command, with status and timing
    manifest.json                              index of what else was captured
    logs/maestro.log                           driver log for this flow (~40 KB)
    logs/device-logcat.txt                     Android only
    screen-hierarchy/step-NNN-<command>.json   captured only where an assertion missed
    screenshots/step-NNN-<command>.png         captured only where an assertion missed
```

Download one with:

```sh
gh run download <run-id> -R ksharma-xyz/KRAIL -n maestro-nightly-android-<run-id> -D ./triage
```

## What the artifact does not contain

Three gaps worth knowing before you go looking for something that was never captured.

- **No video.** Nothing in `.maestro/` calls `startRecording`, and plain `maestro test` records
  nothing. A transient — a dialog that flashed, an animation caught mid-flight — leaves no
  trace beyond whatever the hierarchy dump happened to catch.
- **No screenshots of a passing step.** Maestro captures a frame where an assertion *missed*,
  not at milestones. A screen that renders wrong while every assertion still passes (wrong
  theme ink, clipped text at large font scale, panes overlapping) produces a green run and no
  image. E2E is not visual regression cover here.
- **No device log on iOS.** The iOS leg copies `~/.maestro/tests` and nothing else, so there is
  no `simctl spawn log` equivalent and no crash report. Android gets logcat twice — per flow in
  `logs/device-logcat.txt`, and whole-run in `logcat-android.txt`. An iOS failure is diagnosed
  on hierarchy plus timings alone.

## The order to read it in

1. **`commands.json`** — the spine. A list of entries, each with `metadata.status`
   (`COMPLETED` / `FAILED`), `timestamp`, `duration`, `sequenceNumber`, and the command with
   its variables already resolved. Find the first non-`COMPLETED` entry: that is the step, and
   its `duration` says whether it failed fast or hung to its timeout.

   ```sh
   python3 -c "
   import json,sys
   for e in json.load(open(sys.argv[1])):
       m = e['metadata']
       if m['status'] != 'COMPLETED':
           print(m['sequenceNumber'], m['status'], m['duration'], list(e['command'])[0])
   " triage/maestro-output/tests/*/02-plan-trip/commands.json
   ```

2. **`screen-hierarchy/step-NNN-*.json`** — what was actually on screen at that step. More
   useful than the picture, because it is greppable: node text, `resource-id`, bounds. If the
   element the flow wanted is absent, this says so; if it is present with an empty
   `resource-id`, see the signature table below.

3. **`logs/device-logcat.txt`**, then `logcat-android.txt` — app-side crash, exception or ANR.
   Android only. `run-flows.sh` already greps the whole-run log for `FATAL EXCEPTION` and fails
   the step on a hit, so a crash that did not fail a flow still turns the lane red.

4. **`logs/maestro.log`** — driver-side detail: what it polled for, how long, what it retried.
   Read this when the first three agree that the app looked fine.

5. **`screenshots/step-NNN-*.png`** — one frame, taken when the assertion gave up. Useful as
   confirmation, misleading as a starting point: the timeout has already expired by the time
   it is taken, so a screen that recovered shows as recovered.

## Known signatures

Each row cost at least one wrong diagnosis.

| What you see | What it means |
|---|---|
| Non-zero exit, no flow named in the job log | The suite never ran. Maestro does not recurse into subdirectories; pointed at a directory whose flows live one level down it finds nothing and exits non-zero — the same code a real failure uses. `.maestro/config.yaml` names the lane directories for this reason. |
| Two steps time out, and doubling the timeouts changes nothing | A blocker, not a budget. Something is covering the element. Suspect a sheet, dialog or permission prompt, and go to the hierarchy dump. |
| A node with the right label and an **empty `resource-id`** | `testTagsAsResourceId` is not reaching that window. `exposeTestTagsToUiAutomation()` is called on `KrailNavHost`; anything rendering in its own window (`ModalBottomSheet`, `Dialog`, `Popup`) is outside that subtree and needs it set on the component. An empty string here is the only trace this leaves. |
| **Empty hierarchy + black screenshot** | The emulator's compositor stalling under load. Not the app, not the NSW API. Unexplained as of 2026-08-22 and tracked as the open box in the learning entry; the nightly's single retry usually clears it. |
| ANR naming `com.google.android.*`, KRAIL absent from the CPU table | Wrong system image. The lane runs `google_atd`, not `google_apis`, precisely because the full Google app suite saturates a two-core runner. If you changed the image, change it back. |
| A flow passes locally and fails in CI, first run only | Local device state. CI is always first-run: `pm clear` before reproducing, or the first-run sheets CI hits will stay invisible to you. |
| iOS-only failure on `03-rotation-sweep` | Expected. `setOrientation` is a no-op on the simulator — reports success, app stays portrait, hierarchy empties for that moment. Tagged `android-only` and excluded on the iOS leg. |

## Reproducing locally

```sh
adb shell pm clear xyz.ksharma.krail.debug        # CI is always a first run
maestro test -e APP_ID=xyz.ksharma.krail.debug .maestro/smoke/

# One flow, on a chosen device — pass --device with more than one attached, or
# Maestro shards the flows across all of them and fails in ways that look like app bugs.
maestro --device emulator-5554 test -e APP_ID=... .maestro/smoke/02-plan-trip.yaml

# What the driver actually sees right now
maestro --device emulator-5554 hierarchy | grep '"resource-id"' | sort -u
```

Full lane and selector conventions live in [`.maestro/README.md`](../.maestro/README.md). The
CI-only reasoning — why the settle pre-flight waits for a CPU plateau rather than a threshold,
why the collection has to happen inside the emulator action's own `script:` — is in
[`.maestro/ci/run-flows.sh`](../.maestro/ci/run-flows.sh).

## When it was a real bug

Add a [`docs/learning/`](learning/README.md) entry if the finding went wrong on the way — and
if the signature is reusable, add a row to the table above rather than only writing the story.

Related entries:

- [A lane that never ran](learning/2026-08-22-a-lane-that-never-ran.md) — three stacked CI bugs,
  each hiding the next.
- [Two platforms, two vocabularies](learning/2026-08-22-two-platforms-two-vocabularies.md) —
  expect/actual behavioural drift, which E2E is well placed to catch and unit tests are not.
