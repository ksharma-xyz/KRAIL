# KRAIL Capture Flows

These Maestro flows arrange deterministic app states. They are committed so the
listing can be rebuilt after product changes without reconstructing the process
from screenshots or chat history.

## Prerequisites

1. Build and install the latest branch on the target simulator
2. Seed the canonical trips and parking data listed in `../README.md`
3. Grant location and set Sydney GPS before any map capture
4. Use exactly one booted iOS simulator during a Maestro run
5. Return the app to Saved Trips after timetable captures to stop polling

An iOS simulator build can be produced with the shared `iosApp` scheme and then
installed with `xcrun simctl install`. Xcode may also be used, but the installed
build must come from the commit being captured.

## iOS preflight

```bash
export KRAIL_IOS_UDID="<target simulator UDID>"
xcrun simctl privacy "$KRAIL_IOS_UDID" grant location xyz.ksharma.krail
xcrun simctl privacy "$KRAIL_IOS_UDID" grant location-always xyz.ksharma.krail
xcrun simctl location "$KRAIL_IOS_UDID" set -33.8688,151.2093
```

Use the framework runner so interaction and pixels cannot come from different
simulators:

```bash
store-listing/framework/capture-ios.sh \
  "$KRAIL_IOS_UDID" \
  store-listing/krail/capture-flows/ipad13-capture-service-alerts.yaml \
  store-listing/krail/screenshots/ipad13/04_service_alerts.png \
  2064 2752
```

For iPhone 6.5-inch use `1242 2688`. The runner shuts down other booted iOS
simulators, executes Maestro with the requested UDID, takes the final screenshot
with `simctl`, and rejects a dimension mismatch.

## Capture order

Run Saved Trips first, then live times, parking, service alerts, delays, planning
and dark mode. Flows that open a timetable or sheet must be paired with the
matching return flow. The final device state must be Saved Trips.

Every final capture flow must assert its visible proof and assert that
`Location Permission Required` is not visible before writing a screenshot.

## Failure recovery

- Wrong iOS dimensions: shut down every other iOS simulator and rerun through
  `capture-ios.sh`
- Correct dimensions but wrong app state: inspect the latest Maestro hierarchy,
  prefer its accessibility label over coordinates, and add an assertion
- Permission banner: grant both location services, set Sydney GPS, restart the
  app, and assert the banner is absent
- API polling: close the sheet, navigate Back to Saved Trips, or shut down the
  emulator after capture
- Theme mismatch: open Settings, choose Change Theme, select the product theme,
  then select Light or Dark explicitly
