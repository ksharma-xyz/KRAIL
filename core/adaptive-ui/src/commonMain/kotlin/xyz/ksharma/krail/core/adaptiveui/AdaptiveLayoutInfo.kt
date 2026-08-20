package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import xyz.ksharma.krail.core.log.log

/**
 * Material Design 3 adaptive layout breakpoints and utilities.
 *
 * Based on Material Design 3 guidelines:
 * https://m3.material.io/foundations/layout/applying-layout/window-size-classes
 */
object AdaptiveBreakpoints {

    // Width breakpoints
    /** Compact width: width < 600dp (99.96% of phones in portrait) */
    const val COMPACT_WIDTH = 600

    /** Medium width: 600dp ≤ width < 840dp (93.73% of tablets in portrait) */
    const val MEDIUM_WIDTH = 840

    /** Expanded width: 840dp ≤ width < 1200dp (97.22% of tablets in landscape) */
    const val EXPANDED_WIDTH = 1200

    /** Large width: 1200dp ≤ width < 1600dp (Large tablet displays) */
    const val LARGE_WIDTH = 1600

    // Height breakpoints
    /** Compact height: height < 480dp (99.78% of phones in landscape) */
    const val COMPACT_HEIGHT = 480

    /** Medium height: 480dp ≤ height < 900dp (96.56% of tablets in landscape, 97.59% of phones in portrait) */
    const val MEDIUM_HEIGHT = 900
}

/**
 * Width size class categories based on Material Design 3.
 */
enum class WindowWidthSizeClass {
    /** Width < 600dp - Phones in portrait */
    COMPACT,

    /** 600dp ≤ width < 840dp - Tablets in portrait, large unfolded displays in portrait */
    MEDIUM,

    /** 840dp ≤ width < 1200dp - Tablets in landscape, large unfolded displays in landscape */
    EXPANDED,

    /** 1200dp ≤ width < 1600dp - Large tablet displays */
    LARGE,

    /** Width ≥ 1600dp - Desktop displays */
    EXTRA_LARGE,
}

/**
 * Height size class categories based on Material Design 3.
 */
enum class WindowHeightSizeClass {
    /** Height < 480dp - Phones in landscape */
    COMPACT,

    /** 480dp ≤ height < 900dp - Tablets in landscape, phones in portrait */
    MEDIUM,

    /** Height ≥ 900dp - Tablets in portrait */
    EXPANDED,
}

/**
 * Contains adaptive layout information based on current window size.
 * Provides convenient properties for determining layout strategy.
 */
@Immutable
data class AdaptiveLayoutInfo(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
    val widthDp: Int,
    val heightDp: Int,
) {
    /**
     * True if width is compact (< 600dp) - typically phones in portrait.
     */
    val isCompactWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.COMPACT

    /**
     * True if width is medium (600dp-840dp) - typically tablets in portrait.
     */
    val isMediumWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.MEDIUM

    /**
     * True if width is expanded (840dp-1200dp) - typically tablets in landscape.
     */
    val isExpandedWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.EXPANDED

    /**
     * True if width is large (1200dp-1600dp) - large tablet displays.
     */
    val isLargeWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.LARGE

    /**
     * True if width is extra-large (≥ 1600dp) - desktop displays.
     */
    val isExtraLargeWidth: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.EXTRA_LARGE

    /**
     * True if height is compact (< 480dp) - typically phones in landscape.
     */
    val isCompactHeight: Boolean
        get() = heightSizeClass == WindowHeightSizeClass.COMPACT

    /**
     * True if height is medium (480dp-900dp) - tablets in landscape, phones in portrait.
     */
    val isMediumHeight: Boolean
        get() = heightSizeClass == WindowHeightSizeClass.MEDIUM

    /**
     * True if height is expanded (≥ 900dp) - tablets in portrait.
     */
    val isExpandedHeight: Boolean
        get() = heightSizeClass == WindowHeightSizeClass.EXPANDED

    /**
     * True for tablet and foldable layouts (medium width or larger).
     * Use this to determine when to show dual-pane layouts.
     */
    val isTabletOrFoldable: Boolean
        get() = widthSizeClass != WindowWidthSizeClass.COMPACT

    /**
     * True for desktop layouts (large width or larger).
     */
    val isDesktop: Boolean
        get() = widthSizeClass == WindowWidthSizeClass.LARGE ||
            widthSizeClass == WindowWidthSizeClass.EXTRA_LARGE

    /**
     * True if dual-pane layout should be used (medium width or larger).
     * For SearchStopScreen: List on left, Map on right.
     */
    val shouldShowDualPane: Boolean
        get() = isTabletOrFoldable

    /**
     * Phone in landscape: any window with compact height (< 480 dp).
     *
     * Width alone can't distinguish phone-landscape from tablet — modern phones with
     * edge-to-edge displays hit EXPANDED width (≥ 840 dp) in landscape. But no tablet
     * has < 480 dp height in any orientation, so [isCompactHeight] is the reliable
     * signal for "this is a phone rotated to landscape, treat vertical space as scarce".
     */
    val isPhoneLandscape: Boolean
        get() = isCompactHeight

    /**
     * Tablet (or unfolded foldable) — wide enough for dual-pane AND tall enough that
     * it isn't a phone in landscape. Used to gate adaptations that only make sense
     * with both dimensions roomy (e.g. "always show the full bottom search row").
     */
    val isTablet: Boolean
        get() = !isCompactHeight && widthSizeClass != WindowWidthSizeClass.COMPACT
}

/**
 * The breakpoint contract as a pure function, so it can be pinned at its exact boundaries.
 *
 * Deliberately separate from [rememberAdaptiveLayoutInfo]. The classification is the part with
 * a contract (documented in `docs/TABLET_FOLDABLE_UX.md` §1 and depended on by every screen that
 * splits into panes); reading the window size is the part that needs a composition. Keeping them
 * apart means a test can drive 599 dp and 600 dp directly rather than restating the `when`
 * blocks in a copy that could drift from the real ones.
 *
 * Every boundary is `<`, so the named constant is the FIRST value of the next class up:
 * 600 dp is MEDIUM, not COMPACT.
 */
fun adaptiveLayoutInfoFor(widthDp: Int, heightDp: Int): AdaptiveLayoutInfo {
    val widthSizeClass = when {
        widthDp < AdaptiveBreakpoints.COMPACT_WIDTH -> WindowWidthSizeClass.COMPACT
        widthDp < AdaptiveBreakpoints.MEDIUM_WIDTH -> WindowWidthSizeClass.MEDIUM
        widthDp < AdaptiveBreakpoints.EXPANDED_WIDTH -> WindowWidthSizeClass.EXPANDED
        widthDp < AdaptiveBreakpoints.LARGE_WIDTH -> WindowWidthSizeClass.LARGE
        else -> WindowWidthSizeClass.EXTRA_LARGE
    }

    val heightSizeClass = when {
        heightDp < AdaptiveBreakpoints.COMPACT_HEIGHT -> WindowHeightSizeClass.COMPACT
        heightDp < AdaptiveBreakpoints.MEDIUM_HEIGHT -> WindowHeightSizeClass.MEDIUM
        else -> WindowHeightSizeClass.EXPANDED
    }

    return AdaptiveLayoutInfo(
        widthSizeClass = widthSizeClass,
        heightSizeClass = heightSizeClass,
        widthDp = widthDp,
        heightDp = heightDp,
    )
}

/**
 * Remember and calculate the current adaptive layout information.
 *
 * @return [AdaptiveLayoutInfo] with current window size classifications and helper properties.
 */
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val info = adaptiveLayoutInfoFor(
        widthDp = windowSizeClass.minWidthDp,
        heightDp = windowSizeClass.minHeightDp,
    )

    SideEffect {
        log(
            "[ADAPTIVE] windowSize=${info.widthDp}×${info.heightDp}dp " +
                "| widthClass=${info.widthSizeClass} heightClass=${info.heightSizeClass} " +
                "| dualPane=${info.shouldShowDualPane}",
        )
    }

    return info
}
