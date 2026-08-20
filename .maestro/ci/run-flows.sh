#!/usr/bin/env bash
#
# Runs a Maestro lane, then collects everything a failure needs to be diagnosed,
# while the emulator is still alive.
#
# This is a file rather than inline YAML for one reason:
# reactivecircus/android-emulator-runner executes its `script:` input ONE LINE AT
# A TIME, each in its own `sh -c`. The job log shows it plainly:
#
#     [command]/usr/bin/sh -c adb logcat -c
#     [command]/usr/bin/sh -c set +e
#     [command]/usr/bin/sh -c maestro test -e APP_ID=... .maestro/smoke/
#
# Shell state does not carry across those invocations, so `set +e` on one line
# cannot protect the command on the next, `$?` on the line after is meaningless,
# and the step aborts the instant any single line exits non-zero. An inline
# "run the flows, then collect the evidence" sequence therefore never reaches the
# collection. That is how the first two runs of this lane both ended with no
# artifact: the upload step ran, found nothing, and passed.
#
# One line in the workflow, one shell in here, all the control flow intact.
#
# Usage: run-flows.sh <app-id> <flows-path> <logcat-file> [device-serial]
#
# CI has exactly one device and omits the serial. Pass one when reproducing a CI
# failure locally: with more than one device attached Maestro shards the flows
# across all of them, which fails in ways that look like app bugs and are not.
#
# MAESTRO_RETRIES (default 0) retries the whole suite that many extra times.
#
# Only the NIGHTLY lane sets it, to 1, matching what the iOS nightly leg already
# does. That lane reports rather than gates, so one retry buys signal without
# hiding a real break: a genuine regression fails twice. The PR lane leaves it at
# 0 on purpose — a flake that can block a merge gets fixed, not re-run.
#
# The emulator-runner action runs its `script:` one line at a time in separate
# shells, so this has to be set as a same-line prefix on the invocation:
#   MAESTRO_RETRIES=1 bash .maestro/ci/run-flows.sh ...

set -u

APP_ID="$1"
FLOWS="$2"
LOGCAT="$3"
DEVICE="${4:-}"
RETRIES="${MAESTRO_RETRIES:-0}"

MAESTRO_ARGS=()
ADB_ARGS=()
if [ -n "$DEVICE" ]; then
  MAESTRO_ARGS+=(--device "$DEVICE")
  ADB_ARGS+=(-s "$DEVICE")
fi

# Deliberately no `set -e`: a failing flow is an expected outcome here, and its
# exit code has to survive until the end of the script.
attempt=0
while true; do
  # Cleared before each attempt so the FATAL EXCEPTION check below reads the
  # attempt that actually decided the result, not a crash from a discarded one.
  adb "${ADB_ARGS[@]}" logcat -c 2>/dev/null || true

  maestro "${MAESTRO_ARGS[@]}" test -e APP_ID="$APP_ID" "$FLOWS"
  MAESTRO_EXIT=$?

  if [ "$MAESTRO_EXIT" -eq 0 ] || [ "$attempt" -ge "$RETRIES" ]; then
    break
  fi
  attempt=$((attempt + 1))
  echo "::warning::Maestro suite failed; retry $attempt of $RETRIES."
  # Stop the app so the retry starts from a launch, not from wherever the failed
  # flow abandoned it.
  adb "${ADB_ARGS[@]}" shell am force-stop "$APP_ID" 2>/dev/null || true
done

# Copied into the workspace, because actions/upload-artifact does not expand `~`
# and would silently upload nothing from a path under the home directory.
adb "${ADB_ARGS[@]}" logcat -d > "$LOGCAT" 2>/dev/null || true
mkdir -p maestro-output
cp -R "$HOME/.maestro/tests" maestro-output/ 2>/dev/null || true

# A crash in a background coroutine does not necessarily fail a flow, so the log
# is checked separately from the flow result.
if grep -q "FATAL EXCEPTION" "$LOGCAT" 2>/dev/null; then
  echo "::error::FATAL EXCEPTION found in logcat"
  grep -A 30 "FATAL EXCEPTION" "$LOGCAT"
  exit 1
fi
echo "No fatal exceptions."

exit "$MAESTRO_EXIT"
