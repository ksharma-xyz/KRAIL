#!/usr/bin/env python3
"""Layout invariants that no compiler, test or detekt rule can see.

The first two checks come from one incident, recorded in
docs/learning/2026-08-16-ime-pan-and-unbounded-column.md. The third comes from its sister
incident, docs/learning/2026-08-16-clipped-inside-its-own-parent.md, where the probe
itself was the thing that was wrong. The fourth comes from the iPhone Duo side inset
incident, recorded in docs/testing/DEVICE_PROFILES.md, and the fifth from its second half, the
Plan your trip sheet, which a sheet's own window hid from every screen level check. The rules
they enforce are explained in docs/LAYOUT_AND_INSETS.md.

Run standalone or via scripts/fullQualityChecks.sh. Exit code 1 on any violation.
"""

from __future__ import annotations

import os
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


# The side inset every floating surface pads by. It reads safeDrawing, which is the one inset that
# carries a side on both platforms, so it names no cutout of its own.
SIDE_SAFE_INSETS_FILE = "taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/modifier/SideSafeInsets.kt"

# The device profile helper, which never ships. It names the cutout to deliver one to a screen under
# test, the way the platform does on an Android phone in landscape, so it is the one other file
# allowed to.
DEVICE_INSETS_FILE = "core/testing/src/androidMain/kotlin/xyz/ksharma/krail/core/testing/device/DeviceInsets.kt"

# taj's ModalBottomSheet: the common file holds SheetContent, which insets a sheet's content at the sides, and each
# platform actual must put its content in it. A sheet's window runs edge to edge, so nothing above it insets content.
SHEET_COMMON_FILE = "taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/components/ModalBottomSheet.kt"
SHEET_ACTUAL_FILES = (
    "taj/src/androidMain/kotlin/xyz/ksharma/krail/taj/components/ModalBottomSheet.android.kt",
    "taj/src/iosMain/kotlin/xyz/ksharma/krail/taj/components/ModalBottomSheet.ios.kt",
)
# The iOS reduced motion overlay draws no Material 3 sheet, so it insets its content itself.
SHEET_IOS_OVERLAY_FILE = SHEET_ACTUAL_FILES[1]

# Source sets that hold tests, matched by directory and never by file name. A test dispatches a
# cutout inset on purpose, to prove what the screen does with it.
TEST_SOURCE_SETS = frozenset(
    {
        "commonTest",
        "androidHostTest",
        "androidUnitTest",
        "androidInstrumentedTest",
        "androidTest",
        "iosTest",
        "jvmTest",
        "test",
        "testFixtures",
    }
)

# Never walked: build output holds generated Kotlin, and the rest is not source.
SKIPPED_DIRS = frozenset({"build", ".git", ".gradle", ".idea", ".worktrees", "node_modules"})

# `WindowInsets.displayCutout`, the `foundation.layout.displayCutout` import, or the platform's
# own `displayCutout` property: any of them is a read of the cutout inset.
DISPLAY_CUTOUT = re.compile(r"\bdisplayCutout\b")


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


def strip_kotlin_comments(text: str, keep_lines: bool = False) -> str:
    """Drops block and line comments so a check reads code rather than prose.

    With [keep_lines] a block comment leaves its newlines behind, so a line number counted in the
    result is the line number in the file. Without it the text is shorter, which is fine for a
    check that only asks whether something appears.
    """
    text = re.sub(
        r"/\*.*?\*/",
        (lambda match: "\n" * match.group(0).count("\n")) if keep_lines else "",
        text,
        flags=re.DOTALL,
    )
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


def non_test_kotlin_files(root: Path):
    """Every Kotlin file under [root] that is not in a test source set or build output."""
    for current, dirnames, filenames in os.walk(root):
        dirnames[:] = [d for d in dirnames if d not in SKIPPED_DIRS and d not in TEST_SOURCE_SETS]
        for name in filenames:
            if name.endswith(".kt"):
                yield Path(current) / name


def check_no_raw_display_cutout(root: Path = ROOT) -> list[str]:
    """No screen pads by the display cutout. The shared side modifier and the test helper are the exceptions.

    `WindowInsets.displayCutout.only(Horizontal)` is the inset Android reports a side inset as,
    and on iOS it is zero at the sides. Compose on iOS put the iPhone Duo's 84 pt side strip in
    `systemBars` and `safeDrawing` and reported `displayCutout` as 0, so Search Stop's cards ran
    under the status pill and the swap and go controls could not be tapped. Every Android host
    test, golden and Maestro run passed, because Android delivers the side inset as the cutout.

    Comments are stripped first, so a KDoc that explains why the cutout is banned is fine. Test
    source sets are skipped: a test dispatches a cutout inset on purpose.
    """
    problems: list[str] = []
    allowed = {(root / SIDE_SAFE_INSETS_FILE).resolve(), (root / DEVICE_INSETS_FILE).resolve()}
    for path in non_test_kotlin_files(root):
        if path.resolve() in allowed:
            continue
        code = strip_kotlin_comments(path.read_text(), keep_lines=True)
        for number, line in enumerate(code.splitlines(), start=1):
            if not DISPLAY_CUTOUT.search(line):
                continue
            rel = path.relative_to(root)
            problems.append(
                f"{rel}:{number}: reads the display cutout inset. Compose on iOS reports displayCutout "
                "as 0 at the sides and carries the side safe area in systemBars and safeDrawing "
                "(measured on the iPhone Duo outer screen), so a surface padded by the cutout runs "
                "under the system's status pill there while every Android test passes. Pad with "
                "Modifier.sideSafeInsets() from taj, which reads safeDrawing. See "
                "docs/LAYOUT_AND_INSETS.md 1.6 and docs/testing/DEVICE_PROFILES.md."
            )
    return problems


def check_sheet_content_is_inset(root: Path = ROOT) -> list[str]:
    """Every sheet puts its content in `SheetContent`, and `SheetContent` insets it at the sides.

    A `ModalBottomSheet` opens its own window, which runs edge to edge, and Material 3 pads the content by the vertical
    insets only, so a sheet as wide as a narrow window (the outer screen of the iPhone Duo) put its content under the
    status pill: the Reset button of the Plan your trip sheet opened Control Center. No screen level inset reaches it,
    and the host tests cannot show a sheet window, so what keeps the content clear is that both platform sheets go
    through `SheetContent` and that it calls `sideSafeInsets`. This reads the three files and fails when either link is
    gone. The behaviour itself is proved by `ModalSheetContentDeviceMatrixTest` and `DateTimeSelectorDeviceMatrixTest`.

    Comments are stripped first, so a KDoc that names `SheetContent` does not count as a call.
    """
    problems: list[str] = []

    def code_of(relative: str) -> str | None:
        path = root / relative
        if not path.is_file():
            problems.append(
                f"{relative}: missing, so the sheet content inset cannot be checked. "
                f"Update the paths in {Path(__file__).name}."
            )
            return None
        return strip_kotlin_comments(path.read_text())

    common = code_of(SHEET_COMMON_FILE)
    if common is not None and not re.search(r"\bsideSafeInsets\(", common):
        problems.append(
            f"{SHEET_COMMON_FILE}: SheetContent no longer calls sideSafeInsets. A sheet's window runs edge to edge, so "
            "its content runs under the system's side strips (the iPhone Duo status pill) and a control there takes no "
            "touch. See docs/LAYOUT_AND_INSETS.md 1.6."
        )
    for relative in SHEET_ACTUAL_FILES:
        code = code_of(relative)
        if code is not None and not re.search(r"\bSheetContent\(", code):
            problems.append(
                f"{relative}: the Material 3 sheet's content is not put in SheetContent, so it is not kept out of the "
                "side strips. Wrap it: SheetContent(frame = sheetFrame, content = content)."
            )
    overlay = code_of(SHEET_IOS_OVERLAY_FILE)
    if overlay is not None and not re.search(r"\bsideSafeInsets\(", overlay):
        problems.append(
            f"{SHEET_IOS_OVERLAY_FILE}: the reduced motion overlay no longer pads its content with sideSafeInsets()."
        )
    return problems


def main() -> int:
    problems = (
        check_manifest_adjust_resize()
        + check_single_ime_authority()
        + check_layout_probes_assert_display()
        + check_no_raw_display_cutout()
        + check_sheet_content_is_inset()
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
