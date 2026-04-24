#!/bin/bash
# fullQualityChecks — compile Android + iOS, run Detekt.
# Usage: ./scripts/fullQualityChecks.sh

set -e

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

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
