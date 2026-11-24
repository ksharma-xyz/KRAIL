#!/usr/bin/env bash
#
# Runs the weekly device flows (.maestro/weekly/) against one iOS simulator you have already booted, the same way the weekly
# CI run does, so a device type the CI runner image cannot create, such as the iPhone Duo, can be run from a desk.
#
#   scripts/run_device_matrix_locally.sh <simulator-udid>
#   scripts/run_device_matrix_locally.sh <simulator-udid> .maestro/weekly/03-search-stop-controls.yaml
#
# It does NOT boot, create, erase or install anything. It checks that the simulator you named is booted and that the app is
# installed on it, and stops with the command to run if either is not so. Booting is yours to do, because a simulator on a
# desk is often in use for something else, and this script must never take one over.
#
# Booted simulators and their ids:   xcrun simctl list devices booted
# A device type on this Mac:         xcrun simctl list devicetypes | grep -i duo
# Install the app you want to test:  xcrun simctl install <udid> path/to/iosApp.app
#
# Environment:
#   APP_ID           the app's bundle id (default xyz.ksharma.krail, the iOS id)
#   MAESTRO_RETRIES  extra attempts at the whole suite (default 0, which is right for finding out what is broken)
#   OUT_DIR          where Maestro writes (default build/maestro-weekly/<udid>, which is under the ignored build folder)
#
# The flows start with 01-intro-buttons, which erases the app's data to walk the first-run Intro, and every later flow
# expects the Intro to be finished. To run one flow on its own, finish the Intro by hand or run 01 first.
#
# Exit status: 0 when every flow passed, 1 when a flow failed, 2 for bad usage or a simulator that is not ready.

set -u

usage() { sed -n '2,25p' "$0" | sed 's/^# \{0,1\}//'; }

case "${1:-}" in
  ""|-h|--help) usage; exit 2 ;;
esac

UDID="$1"
FLOWS="${2:-.maestro/weekly/}"
APP_ID="${APP_ID:-xyz.ksharma.krail}"
OUT_DIR="${OUT_DIR:-build/maestro-weekly/$UDID}"

# Run from the repository root, wherever this was started from, because the flows, the helpers and the default output
# directory are all relative to it.
cd "$(dirname "$0")/.." || exit 2

if ! command -v maestro >/dev/null 2>&1; then
  echo "run_device_matrix_locally: maestro is not on PATH. See .maestro/README.md to install it." >&2
  exit 2
fi
if ! command -v xcrun >/dev/null 2>&1; then
  echo "run_device_matrix_locally: xcrun is not available. This script drives iOS simulators and needs Xcode." >&2
  exit 2
fi
if [ ! -e "$FLOWS" ]; then
  echo "run_device_matrix_locally: $FLOWS does not exist." >&2
  exit 2
fi

# A read only look at the simulator list. The state is the last parenthesis on the device's line.
device_line="$(xcrun simctl list devices | grep -F "$UDID" | head -1 | sed 's/^[[:space:]]*//')"
if [ -z "$device_line" ]; then
  echo "run_device_matrix_locally: no simulator with id $UDID. List them with: xcrun simctl list devices" >&2
  exit 2
fi
case "$device_line" in
  *"(Booted)"*) ;;
  *)
    echo "run_device_matrix_locally: the simulator is not booted: $device_line" >&2
    echo "Boot it yourself, then run this again:  xcrun simctl boot $UDID && xcrun simctl bootstatus $UDID -b" >&2
    exit 2
    ;;
esac

# The app has to be on it already. `get_app_container` only looks, and fails when the bundle is not installed.
if ! xcrun simctl get_app_container "$UDID" "$APP_ID" >/dev/null 2>&1; then
  echo "run_device_matrix_locally: $APP_ID is not installed on $UDID." >&2
  echo "Install a simulator build of the app:  xcrun simctl install $UDID <path to iosApp.app>" >&2
  exit 2
fi

echo "Simulator: $device_line"
echo "App:       $APP_ID"
echo "Flows:     $FLOWS"
echo "Output:    $OUT_DIR"

# One retry stops the app and runs the suite again. It never restarts the simulator here, which would boot it.
bash .maestro/ci/run-weekly-ios.sh "$UDID" "$APP_ID" "$FLOWS" "$OUT_DIR"
code=$?

echo
echo "== Result =="
# The table lists the flows of the weekly folder. A single flow run shows the others as not run, which is true.
bash .maestro/ci/weekly-summary.sh "$OUT_DIR" .maestro/weekly

if [ "$code" -ne 0 ]; then
  echo
  echo "Failed. What Maestro wrote is in $OUT_DIR, in the order to read it in: docs/MAESTRO_TRIAGE.md."
  exit 1
fi
exit 0
