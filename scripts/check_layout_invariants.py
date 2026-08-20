#!/usr/bin/env python3
"""Layout invariants that no compiler, test or detekt rule can see.

The first two checks come from one incident, recorded in
docs/learning/2026-08-16-ime-pan-and-unbounded-column.md. The third comes from its sister
incident, docs/learning/2026-08-16-clipped-inside-its-own-parent.md, where the probe
itself was the thing that was wrong. The rules they enforce are explained in
docs/LAYOUT_AND_INSETS.md.

Run standalone or via scripts/fullQualityChecks.sh. Exit code 1 on any violation.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

MANIFEST = ROOT / "androidApp/src/main/AndroidManifest.xml"

# safeDrawing already includes the IME, so a file holding both of these is applying the
# keyboard inset twice unless one of them is deliberately scoped elsewhere.
IME_AUTHORITIES = ("safeDrawingPadding()", "imePadding()")

SEARCH_ROOTS = (
    "feature",
    "composeApp",
    "taj",
)

# A layout probe answers "can the rider see this". Bounds cannot answer it.
BOUNDS_PROBE = "getUnclippedBoundsInRoot()"

# Wider than SEARCH_ROOTS on purpose: a layout probe can live in any module.
TEST_SEARCH_ROOTS = (
    "feature",
    "composeApp",
    "taj",
    "core",
)


def check_manifest_adjust_resize() -> list[str]:
    """MainActivity must ask the system NOT to pan the window for the keyboard.

    The app is edge to edge and pads for the IME in Compose. Without an explicit
    adjustResize the mode defaults to ADJUST_UNSPECIFIED, the system resolves it to pan,
    and the window slides up on top of the padding Compose already applied.
    """
    if not MANIFEST.exists():
        return [f"{MANIFEST}: manifest not found"]

    text = MANIFEST.read_text()
    activity = re.search(
        r"<activity[^>]*android:name=\"\.MainActivity\".*?>",
        text,
        re.DOTALL,
    )
    if activity is None:
        return [f"{MANIFEST}: MainActivity block not found"]

    if 'android:windowSoftInputMode="adjustResize"' in activity.group(0):
        return []

    return [
        f"{MANIFEST}: MainActivity must declare "
        'android:windowSoftInputMode="adjustResize". Without it the system pans the '
        "window for the keyboard on top of Compose's own IME padding, and bottom "
        "anchored inputs are drawn a keyboard's height above the keyboard. "
        "See docs/LAYOUT_AND_INSETS.md."
    ]


def check_single_ime_authority() -> list[str]:
    """One composable applies the keyboard inset per screen, never two in one file.

    Advisory by nature: a file legitimately holding two independent surfaces can trip
    this. Add the file to ALLOWED below with a comment saying why, rather than deleting
    the check.
    """
    # SavedTripsScreen hosts two surfaces: the home screen's own ime-padded box, and the
    # Ask KRAIL cover, which is a sibling of it and owns its inset separately. AskKrailScreen
    # pads its content column with safeDrawing and anchors the cloud field with imePadding, so
    # the light ends where the keyboard starts. Two different jobs, not two authorities.
    allowed = {"SavedTripsScreen.kt", "AskKrailScreen.kt"}

    problems: list[str] = []
    for root in SEARCH_ROOTS:
        for path in (ROOT / root).rglob("*.kt"):
            if path.name in allowed:
                continue
            text = path.read_text()
            if all(token in text for token in IME_AUTHORITIES):
                rel = path.relative_to(ROOT)
                problems.append(
                    f"{rel}: applies both safeDrawingPadding() and imePadding(). "
                    "safeDrawing already includes the IME, so this pads for the keyboard "
                    "twice. One inset authority per screen: see docs/LAYOUT_AND_INSETS.md."
                )
    return problems


def strip_kotlin_comments(text: str) -> str:
    """Drops block and line comments so a check reads code rather than prose."""
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.DOTALL)
    return re.sub(r"//[^\n]*", "", text)


def check_layout_probes_assert_display() -> list[str]:
    """A `*LayoutTest` proves visibility with `assertIsDisplayed()`, never with bounds.

    `getUnclippedBoundsInRoot()` reports where a node was LAID OUT. A node clipped by an
    ancestor returns perfectly legal on-screen bounds, which is the exact shape of the bug
    layout probes exist to catch: the send button was inside its own bar, below the bar's
    bottom edge, with bounds that read as fine. A bounds assertion went green against it
    and stopped the investigation.

    Scoped to files named `*LayoutTest.kt` deliberately. Bounds are a legitimate tool for
    measuring spacing or alignment; they are not a tool for "can the rider see this", and
    a file that calls itself a layout probe is claiming to answer that question.
    """
    problems: list[str] = []
    for root in TEST_SEARCH_ROOTS:
        for path in (ROOT / root).rglob("*LayoutTest.kt"):
            # Comments stripped first. Naming the banned API in a KDoc that explains why it
            # is banned is the correct thing to do, and a check that punished it would push
            # the explanation out of the file it belongs in.
            if BOUNDS_PROBE not in strip_kotlin_comments(path.read_text()):
                continue
            rel = path.relative_to(ROOT)
            problems.append(
                f"{rel}: a layout probe uses {BOUNDS_PROBE}. Bounds say where a node was "
                "laid out, not whether any of it is on screen — a clipped node returns "
                "legal bounds and the assertion passes against a live bug. Use "
                "assertIsDisplayed(), which walks the clip chain, and mutation-check the "
                "probe in both directions. See docs/LAYOUT_AND_INSETS.md §1.4 and "
                "docs/learning/2026-08-16-clipped-inside-its-own-parent.md."
            )
    return problems


def main() -> int:
    problems = (
        check_manifest_adjust_resize()
        + check_single_ime_authority()
        + check_layout_probes_assert_display()
    )

    if problems:
        print("Layout invariant check FAILED:\n")
        for problem in problems:
            print(f"  - {problem}\n")
        return 1

    print("Layout invariants OK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
