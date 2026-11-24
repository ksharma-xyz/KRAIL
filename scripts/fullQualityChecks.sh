#!/bin/bash
# fullQualityChecks — structural guards, compile Android + iOS, run Detekt.
# Usage: ./scripts/fullQualityChecks.sh
#
# This script is the documented pre-PR gate, so it has to run everything CI can
# fail on that is not a full build. It previously did not, and a branch that was
# green here failed `code-quality / detekt` on a task this script never invoked.
# When a script or verification task is added to .github/workflows/code-quality.yml, add it to
# scripts/ci_fast_checks.sh in the same change; that one list feeds this script and the pre-push hook.

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

# Idempotent and silent when nothing changed. Here so that any clone which runs the pre-PR
# gate once also gets the pre-push hook, rather than depending on someone remembering to.
./scripts/install_git_hooks.sh

echo "▶ Fast CI checks (script steps, then test wiring and lane classification)..."
# The steps code-quality.yml runs before detekt, from the one list in scripts/ci_fast_checks.sh that
# the pre-push hook also uses, so the two cannot drift apart. The script steps (layout invariants,
# analytics assumptions) come first and the four verify tasks after them; both run before the
# compiles on purpose, because each is a seconds-long structural check that should fail in seconds
# rather than after two full compiles. check_stale_assumptions.py warns without failing, as in CI.
./scripts/ci_fast_checks.sh

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
