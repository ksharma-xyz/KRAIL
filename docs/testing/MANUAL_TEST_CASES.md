# Manual test cases

Scenarios that have to be run by a person, because no automated layer in this repo can
reach them.

This is a **catalogue to run against**, not a checklist to complete before every PR. The
per-change checklist lives in `CLAUDE.md` ("QA checklist before handing over code") and is
about the screen you just touched. This file is for sanity passes: before a release, after
a change to networking or lifecycle, or when a rider reports something none of the tests
predicted.

---

## What belongs here, and what does not

A case earns a place here only if **every** automated layer is genuinely unable to reach it.
Before adding one, rule out each layer:

| Layer | Covers | Why a case might still escape it |
|---|---|---|
| Unit / host tests | Logic, state machines, mappers | Cannot reach OS callbacks or platform services |
| Compose interaction tests | Rendering, input, state survival | No real window, no real network, no real radio |
| Screenshot tests | Pixel-level appearance | Static; no transitions, no timing |
| Maestro smoke | Launch, plan a trip | Gated on a PR, so kept fast and stable |
| Maestro nightly | Rotation, background/foreground, kill/relaunch, permission denial | Report-only; runs on a device farm with a normal network |
| Guard tests | Registers and structure | Text scans; cannot observe runtime |

**If a case can be automated, automate it instead.** `docs/learning/README.md` puts it
plainly: prefer a check over a paragraph. A manual case that could have been a test is a
case that will stop being run.

The cases below survive that filter for one of three reasons:

1. **The OS state cannot be faked** — airplane mode, a captive portal, a marginal radio.
2. **The hardware is not in CI** — a real iPhone, a foldable, a tablet.
3. **The observation is a judgement** — does this transition look broken, is this contrast
   readable at the largest font scale.

---

## How to record a run

Copy the table for the section you ran into the PR or issue, and fill the result column.
An unrecorded manual pass is indistinguishable from one that never happened.

Use these three values, and nothing vaguer:

- **pass** — observed the expected behaviour
- **fail** — observed something else; say what, with the log line or a screenshot
- **blocked** — could not run it; say why (no hardware, no portal to hand)

`blocked` is a real result and more useful than a skipped row. Several cases below are
permanently blocked on hardware nobody has yet, and saying so is the point.

---

## 1. Network classification

Added with the offline-aware networking work. Read
[`docs/NETWORK_RELIABILITY.md`](../NETWORK_RELIABILITY.md) first: the taxonomy table there
is what these cases verify, and its **Verification status** section records which rows have
never been observed on a device.

The Android rows are covered by `NetworkErrorClassifierTest`, which drives a real Ktor
client at unroutable addresses. These cases exist for what that cannot reach: the actual
radio, and iOS at all.

### 1.1 Android, airplane mode

Emulator or device. `adb` commands are in `CLAUDE.md`.

| # | Steps | Expect | Result |
|---|---|---|---|
| 1.1.1 | Open a timetable with results showing, enable airplane mode, wait for the auto-refresh | Logcat shows `network call failed: offline (transportDown=true)`. Journeys stay on screen; the board does not flip to an error state | |
| 1.1.2 | Time the gap between the request log line and the failure line | Well under a second. A multi-second gap means the retry gate is not declining and connect timeouts are being burned | |
| 1.1.3 | Disable airplane mode | `KrailConnectivity: transport=Up` followed within a tick by `Transport returned after an offline failure, refetching`. No tap needed | |
| 1.1.4 | Repeat 1.1.1 but rotate the device while offline | No crash, no process restart, and exactly **one** refetch on reconnect. Two means the reconnect is firing on resubscription | |
| 1.1.5 | Open a departure board, enable airplane mode, leave it 3 minutes | Polling stops. Logcat shows no repeated request lines while transport is down | |
| 1.1.7 | Open a **timetable** (not a departure board), enable airplane mode, leave it 2 minutes | Known asymmetry: the timetable auto-refresh is **not** connectivity-gated and keeps firing every 30s. Each attempt fails fast because retry declines, so it is cheap, but it is not the departure board's behaviour. Observed on iOS 2026-09-12: two failures, 30s apart | |
| 1.1.6 | Cold-start the app with airplane mode already on | `transport=Down` at launch from the seed read, not after a delay. No offline claim before the first OS callback | |

### 1.2 iOS, airplane mode

Needs real hardware. A simulator shares the Mac's network stack and has no airplane mode,
so the no-transport case cannot be produced on one at all.

**Partly run.** 1.2.1 and 1.2.2 passed on an iPhone 11, iOS 26.6, on 2026-09-12. The rest
of the iOS taxonomy is still written from Apple's documented codes and has never been
observed.

| # | Steps | Expect | Result |
|---|---|---|---|
| 1.2.1 | Real iPhone. Open a timetable, enable airplane mode, wait for the auto-refresh | A failure classified as `offline` | **pass** 2026-09-12 |
| 1.2.2 | Capture the underlying `DarwinHttpRequestException.origin.code` for that failure | Compare against `NSURLErrorNotConnectedToInternet` (-1009). **Correct the taxonomy table if it differs** | **pass** `Code=-1009`, matches |
| 1.2.3 | Join a Wi-Fi with no internet route (hotspot with mobile data off), then request a trip | Classified `unreachable`, not `offline`: the transport is up | |
| 1.2.4 | Capture `origin.code` for 1.2.3 | Compare against `NSURLErrorCannotFindHost` (-1003) / `NSURLErrorDNSLookupFailed` (-1006) | |
| 1.2.5 | Disable airplane mode with the timetable open | Board refills without a tap | **pass** 10 clean auto-refresh cycles over 5 min |
| 1.2.6 | Once 1.2.1 to 1.2.4 pass | Flip the iOS row in the doc's **Verification status** table to yes, and say which build verified it | |

#### Capturing the evidence on iOS

Kermit's debug lines do **not** reach stdout on a device; only `logError` does, because
`Log.ios.kt` calls `NSLog` explicitly. That is enough for these cases, because the
classification line and the underlying throwable are both logged at error level.

```sh
xcrun devicectl list devices                       # find the UDID
cd iosApp && xcodebuild -project iosApp.xcodeproj -scheme iosApp \
  -configuration Debug -destination 'id=<UDID>' -derivedDataPath <dir> build
xcrun devicectl device install app --device <UDID> "<dir>/Build/Products/Debug-iphoneos/Krail App.app"
xcrun devicectl device process launch --device <UDID> --terminate-existing --console xyz.ksharma.krail
```

The last command streams `NSLog` output. `--terminate-existing` matters: attaching to an
already-running process captures nothing.

**Airplane mode cannot be toggled from the Mac.** iOS exposes no API for it and
`devicectl` has no such command, so this case always needs a person holding the phone.

### 1.3 Captive portal

Needs a network that intercepts. A café or hotel Wi-Fi before accepting its terms, or a
local proxy returning `text/html` with a 200.

`CaptivePortalValidatorTest` covers the logic over a MockEngine. What it cannot cover is a
real interception, where the portal may also hijack DNS, serve a redirect chain, or answer
on a different port.

| # | Steps | Expect | Result |
|---|---|---|---|
| 1.3.1 | Join a portal network, do not accept the terms, request a trip | Classified `captive_portal`, not `malformed` and not an NSW outage | |
| 1.3.2 | Same, on a portal that answers HTTP 511 rather than an HTML 200 | Also `captive_portal`. This path is handled in the classifier, not the validator | |
| 1.3.3 | Accept the portal terms, then retry | Normal results. No stale error state left behind | |
| 1.3.4 | Note what the portal actually returned (status, `Content-Type`) | If it was neither HTML nor 511, the validator has a gap worth recording | |

### 1.4 Weak and flapping signal

**The case most likely to produce surprises, and the one nothing automated reproduces.**
Android's `NET_CAPABILITY_VALIDATED` is a probe result, so it can flap on a marginal
connection while the radio stays on.

A train through a tunnel, a lift, or a basement. A Faraday bag works if you have one.

| # | Steps | Expect | Result |
|---|---|---|---|
| 1.4.1 | Ride a line with tunnel sections with the timetable open | Polling suspends and resumes. No crash, no duplicated journeys, no runaway refetching at each transition | |
| 1.4.2 | Watch for repeated `transport=` transitions in a short window | Each genuine reconnection produces at most one refetch. A burst means `distinctUntilChanged` is not collapsing the settling callbacks | |
| 1.4.3 | Let a request start with signal and lose it mid-flight | Classified `offline`, not `unreachable`: the state is read at failure time, not at request time | |
| 1.4.4 | Check battery over a long offline stretch with a screen open | No measurable drain from polling. The loop should be suspended, not spinning | |

### 1.5 Upstream failure

Hard to induce deliberately. Worth running opportunistically during a real NSW incident.

| # | Steps | Expect | Result |
|---|---|---|---|
| 1.5.1 | During an NSW outage, request a trip | Classified `upstream`, and **not** presented as the rider's connection problem | |
| 1.5.2 | Check the retry behaviour in logs | Retried with backoff, capped. Not retried indefinitely | |

---

## 2. Configuration and environment

`CLAUDE.md` requires rotation on every changed screen, and the Maestro nightly sweeps
rotation, background/foreground and kill/relaunch. These cases are the ones that lane does
not run.

| # | Steps | Expect | Result |
|---|---|---|---|
| 2.1 | Largest system font scale, every screen | Nothing clipped, nothing overlapping, no text truncated to meaninglessness | |
| 2.2 | Smallest font scale | No layout collapse, no accidental centring | |
| 2.3 | Switch theme while a screen is mid-animation | No flash of the wrong colour, no stuck intermediate state. The theme transition animates for 1500 ms, see `taj/THEME_COLOUR_ROLES.md` | |
| 2.4 | Dark mode on every screen | Contrast holds. Check any colour drawn in the rider's theme colour specifically | |
| 2.5 | Split screen / multi-window | No crash. Layout degrades sensibly rather than breaking | |
| 2.6 | Foldable, fold and unfold with a screen open | State survives. See `docs/TABLET_FOLDABLE_UX.md` | |
| 2.7 | Tablet, both orientations | Dual-pane behaves per that doc | |
| 2.8 | System back gesture from every screen | Predictable. No skipped screens, no dead ends | |

---

## 3. Permissions and platform services

The nightly lane covers denial. These cover the paths after it.

| # | Steps | Expect | Result |
|---|---|---|---|
| 3.1 | Deny location, then grant it in Settings and return to the app | The app picks up the grant without a restart | |
| 3.2 | Grant location, then revoke it in Settings while the app is backgrounded | No crash on return. Location features degrade rather than fail | |
| 3.3 | Deny microphone, then use the speech input entry point | A clear path forward, not a silent no-op | |
| 3.4 | iOS only: check the app appears in Settings after a permission request | If it does not, the wrapper inspected status without requesting. See the memory note on iOS permission libraries | |

---

## 4. Recording a run

Paste the filled table into the PR or issue, with:

- **Device and OS version** — "Pixel 7, Android 15" or "iPhone 13, iOS 18.4"
- **Build** — commit SHA or version name
- **Date**
- Anything that was `blocked`, and why

### Standing blockers

These are known and not worth rediscovering each pass:

| Case | Blocked on |
|---|---|
| 1.2.3, 1.2.4, 1.2.6 | A real iPhone plus a routeless Wi-Fi. 1.2.1, 1.2.2 and 1.2.5 passed 2026-09-12 |
| 1.3 | A network that intercepts |
| 1.4 | A tunnel, a lift, or a Faraday bag |
| 1.5 | An actual NSW incident |
| 2.6, 2.7 | Foldable and tablet hardware |

If a standing blocker is cleared, delete the row rather than leaving it as an excuse.
