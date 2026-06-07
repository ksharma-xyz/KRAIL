#!/bin/bash
# cut-release.sh — trigger the "1. Cut Release Branch" GitHub Actions workflow.
#
# Usage:
#   ./scripts/cut-release.sh                    # auto-increment minor on main
#   ./scripts/cut-release.sh 1.25.0             # specify explicit next version for main
#   ./scripts/cut-release.sh --base feat/foo    # cut from a branch other than main
#
# What happens after you run this:
#   1. GH Actions creates prod/{current-version} from <base-branch>
#   2. A PR to bump main to <next-version> is opened and auto-merged
#   3. Pushing prod/{version} auto-triggers "2. Deploy RC" → RC1 tag + GP Internal + TestFlight

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

BASE_BRANCH="main"
NEXT_VERSION=""

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)
      BASE_BRANCH="$2"; shift 2 ;;
    --base=*)
      BASE_BRANCH="${1#--base=}"; shift ;;
    -*)
      echo "Unknown flag: $1" >&2; exit 1 ;;
    *)
      NEXT_VERSION="$1"; shift ;;
  esac
done

# Read current version from the base branch (local checkout must be up-to-date)
CURRENT_VERSION=$(sed -n 's/.*versionName = "\([^"]*\)".*/\1/p' "$ROOT/androidApp/build.gradle.kts")
if [[ -z "$CURRENT_VERSION" ]]; then
  echo "error: could not read versionName from androidApp/build.gradle.kts" >&2
  exit 1
fi

NEXT_DISPLAY="${NEXT_VERSION:-auto}"

echo ""
echo "  Release branch : prod/${CURRENT_VERSION}  (from ${BASE_BRANCH})"
echo "  Next on main   : ${NEXT_DISPLAY} (workflow handles bump)"
echo ""
read -r -p "Proceed? [y/N] " CONFIRM
[[ "$CONFIRM" =~ ^[Yy]$ ]] || { echo "Aborted."; exit 0; }

# Only pass next-version if explicitly provided — workflow auto-increments otherwise
EXTRA_FIELDS=()
if [[ -n "$NEXT_VERSION" ]]; then
  EXTRA_FIELDS+=(--field "next-version=${NEXT_VERSION}")
fi

gh workflow run release-1-cut.yml \
  --ref "$BASE_BRANCH" \
  --field "base-branch=${BASE_BRANCH}" \
  "${EXTRA_FIELDS[@]}"

echo ""
echo "Workflow triggered. Track progress at:"
echo "  https://github.com/$(gh repo view --json nameWithOwner -q .nameWithOwner)/actions/workflows/release-1-cut.yml"
