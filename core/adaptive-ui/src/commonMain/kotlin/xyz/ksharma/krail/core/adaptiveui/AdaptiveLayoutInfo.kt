package xyz.ksharma.krail.core.adaptiveui

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

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
}

/**
 * Remember and calculate the current adaptive layout information.
 *
 * @return [AdaptiveLayoutInfo] with current window size classifications and helper properties.
 */
@Composable
fun rememberAdaptiveLayoutInfo(): AdaptiveLayoutInfo {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass

    val widthSizeClass = when {
        windowSizeClass.minWidthDp < AdaptiveBreakpoints.COMPACT_WIDTH ->
            WindowWidthSizeClass.COMPACT
        windowSizeClass.minWidthDp < AdaptiveBreakpoints.MEDIUM_WIDTH ->
            WindowWidthSizeClass.MEDIUM
        windowSizeClass.minWidthDp < AdaptiveBreakpoints.EXPANDED_WIDTH ->
            WindowWidthSizeClass.EXPANDED
        windowSizeClass.minWidthDp < AdaptiveBreakpoints.LARGE_WIDTH ->
            WindowWidthSizeClass.LARGE
        else ->
            WindowWidthSizeClass.EXTRA_LARGE
    }

    val heightSizeClass = when {
        windowSizeClass.minHeightDp < AdaptiveBreakpoints.COMPACT_HEIGHT ->
            WindowHeightSizeClass.COMPACT
        windowSizeClass.minHeightDp < AdaptiveBreakpoints.MEDIUM_HEIGHT ->
            WindowHeightSizeClass.MEDIUM
        else ->
            WindowHeightSizeClass.EXPANDED
    }

    return AdaptiveLayoutInfo(
        widthSizeClass = widthSizeClass,
        heightSizeClass = heightSizeClass,
        widthDp = windowSizeClass.minWidthDp,
        heightDp = windowSizeClass.minHeightDp,
    )
}
