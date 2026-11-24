#!/usr/bin/env bash
#
# Prints a Markdown table of how each weekly flow ended, read from the files Maestro wrote, for a job summary or a terminal.
#
# Usage: weekly-summary.sh <maestro-output-dir> [flows-dir]
#
# <maestro-output-dir> is the directory Maestro was given as `--test-output-dir`, or the copy of `~/.maestro/tests` that
# the Android lane makes. Under it each run is a timestamped folder and each flow in it a folder named for the flow file,
# holding a `commands.json` with a status for every command:
#
#   <maestro-output-dir>/<timestamp>/<flow-name>/commands.json
#
# [flows-dir] is where the flows live (default `.maestro/weekly`). One row is printed for each flow file in it, so a
# flow that never ran shows as "not run" and is not simply missing from the table.
#
# A flow passed when no command in its latest `commands.json` is FAILED. When the suite was retried, the latest run of a flow
# is the one that counts, which is the same rule the retry itself applies: a genuine regression fails twice. The step
# that failed is the last FAILED command, which is the innermost one when the failure was inside a `runFlow`.
#
# This only reads files. It needs `jq`, which is on the macOS and Ubuntu runners, and prints "unknown" rather than failing
# when it is not there, because a missing summary must never turn a green run red.

set -u

OUT="${1:?usage: weekly-summary.sh <maestro-output-dir> [flows-dir]}"
FLOWS="${2:-.maestro/weekly}"

echo "| Flow | Result | Failed step |"
echo "|---|---|---|"

if ! command -v jq >/dev/null 2>&1; then
  echo "| (jq is not installed, so no result can be read) | unknown | |"
  exit 0
fi

found=0
for flow in "$FLOWS"/*.yaml; do
  [ -e "$flow" ] || continue
  found=1
  name="$(basename "$flow" .yaml)"
  # Folder names start with a timestamp, so the last in sort order is the latest run of this flow.
  latest="$(find "$OUT" -type f -path "*/$name/commands.json" 2>/dev/null | sort | tail -1)"
  if [ -z "$latest" ]; then
    echo "| \`$name\` | not run | |"
    continue
  fi
  failed="$(jq -r '
      [ .[] | select(.metadata.status == "FAILED") ]
      | sort_by(.metadata.sequenceNumber)
      | last
      | if . == null then ""
        else ((.command | keys[0]) + " "
              + ((([ .command | .. | objects | (.idRegex // .textRegex // empty) ] | first) // "")))
        end' "$latest" 2>/dev/null | tr '|' '/')"
  if [ -n "$failed" ]; then
    echo "| \`$name\` | FAIL | \`$failed\` |"
  else
    echo "| \`$name\` | pass | |"
  fi
done

if [ "$found" -eq 0 ]; then
  echo "| (no flow files found in $FLOWS) | unknown | |"
fi
exit 0
