#!/bin/bash
# fullQualityChecks — compile Android + iOS, run Detekt.
# Usage: ./scripts/fullQualityChecks.sh

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo "▶ Layout invariants..."
python3 scripts/check_layout_invariants.py

echo ""
echo "▶ Analytics assumptions..."
# Warns only. A date passing is not a reason to block an unrelated build; a scheduled job
# runs this with --strict to raise an issue instead.
python3 scripts/check_stale_assumptions.py

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
