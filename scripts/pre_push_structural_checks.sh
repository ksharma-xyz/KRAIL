#!/usr/bin/env bash
#
# The structural half of ./scripts/fullQualityChecks.sh, fast enough to sit on a push.
#
# These four tasks answer "is the repo still coherent?" rather than "is my change correct?".
# They are the ones a targeted `./gradlew :some:module:testAndroidHostTest` can never find,
# because the defects they catch are in the shape of the build rather than in any one module:
# a module with test sources and no host-test task, a module with shared tests and no iOS lane
# classification, an ad-hoc fake at a boundary that has a real one.
#
# Placed on pre-push rather than pre-commit deliberately. They take seconds, not milliseconds,
# and a commit is cheap to amend while a red CI run is not.
#
# Why this exists at all: `verifyIosTestClassification` failed in CI on a change whose author
# had run a compile, detekt and the touched modules' tests, all green, and skipped the script
# that wraps these. See docs/learning/2026-09-22-the-gate-i-did-not-open.md.
#
# Bypass with `git push --no-verify`, or SKIP_STRUCTURAL_CHECKS=1 when the hook itself is in
# the way. Both are fine; the point is that skipping becomes a decision rather than an
# oversight.

set -euo pipefail

if [ "${SKIP_STRUCTURAL_CHECKS:-0}" = "1" ]; then
  echo "pre-push: structural checks skipped (SKIP_STRUCTURAL_CHECKS=1)"
  exit 0
fi

repo_root="$(git rev-parse --show-toplevel)"
cd "$repo_root"

if [ ! -x ./gradlew ]; then
  echo "pre-push: no ./gradlew here, skipping structural checks"
  exit 0
fi

echo "pre-push: structural checks (test wiring, iOS lane, boundary fakes)..."

# --continue so all four report at once rather than one per run, matching the
# "Verify test wiring" step in code-quality.yml and fullQualityChecks.sh.
#
# -PciQuality is deliberately NOT passed: that flag exists so CI can substitute placeholder
# API keys, and a local run has the real ones in local.properties.
if ./gradlew --quiet \
  verifyTestWiring \
  verifyTestingModuleUsage \
  verifyNoAdHocBoundaryFakes \
  verifyIosTestClassification \
  --continue; then
  echo "pre-push: structural checks passed"
else
  cat >&2 <<'MSG'

pre-push: structural checks failed. Nothing has been pushed.

These are the checks a change-shaped test run cannot find. The usual cause is a module
that grew test sources without a classification, or a new boundary fake.

Fix it, or push with --no-verify if you know why it is failing.
MSG
  exit 1
fi
