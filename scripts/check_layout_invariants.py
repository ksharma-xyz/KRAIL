#!/usr/bin/env python3
"""Layout invariants that no compiler, test or detekt rule can see.

Both checks here come from one incident, recorded in
docs/learning/2026-08-16-ime-pan-and-unbounded-column.md. The rules they enforce are
explained in docs/LAYOUT_AND_INSETS.md.

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


def main() -> int:
    problems = check_manifest_adjust_resize() + check_single_ime_authority()

    if problems:
        print("Layout invariant check FAILED:\n")
        for problem in problems:
            print(f"  - {problem}\n")
        return 1

    print("Layout invariants OK.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
