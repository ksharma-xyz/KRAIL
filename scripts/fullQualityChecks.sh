#!/bin/bash
# fullQualityChecks — structural guards, compile Android + iOS, run Detekt.
# Usage: ./scripts/fullQualityChecks.sh
#
# This script is the documented pre-PR gate, so it has to run everything CI can
# fail on that is not a full build. It previously did not, and a branch that was
# green here failed `code-quality / detekt` on a task this script never invoked.
# When a verification task is added to .github/workflows/code-quality.yml, add it
# here in the same change.

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Idempotent and silent when nothing changed. Here so that any clone which runs the pre-PR
# gate once also gets the pre-push hook, rather than depending on someone remembering to.
./scripts/install_git_hooks.sh

echo "▶ Layout invariants..."
python3 scripts/check_layout_invariants.py

echo ""
echo "▶ Analytics assumptions..."
# Warns only. A date passing is not a reason to block an unrelated build; a scheduled job
# runs this with --strict to raise an issue instead.
python3 scripts/check_stale_assumptions.py

echo ""
echo "▶ Test wiring and lane classification..."
# The same four tasks as the "Verify test wiring" step in code-quality.yml, and
# they run before the compiles on purpose: each is a seconds-long structural
# check, so a module with test sources and no host-test task, or with no iOS lane
# classification, fails in seconds instead of after two full compiles.
#
# --continue matches CI so all four report at once rather than one per run.
# -PciQuality is deliberately NOT passed: that flag exists so CI can substitute
# placeholder API keys, and a local run has the real ones in local.properties.
./gradlew \
  verifyTestWiring \
  verifyTestingModuleUsage \
  verifyNoAdHocBoundaryFakes \
  verifyIosTestClassification \
  --continue

echo ""
echo "▶ Android compile..."
./gradlew compileDebugSources

echo ""
echo "▶ iOS compile (Simulator arm64)..."
./gradlew compileKotlinIosSimulatorArm64

echo ""
echo "▶ Detekt..."
./gradlew detekt --continue

echo ""
echo "✓ All quality checks passed."
