package xyz.ksharma.krail.analytics

import xyz.ksharma.krail.core.appinfo.FoldState

/**
 * The app's own device buckets, kept deliberately small.
 *
 * [FOLDABLE_OPEN] is the only fold value here because it is the only one the window can
 * prove: a foldable running on its cover display reports no hinge inside the window and is
 * indistinguishable from a phone. Rather than guess, it is reported as [PHONE] - and
 * crossed with the device model Firebase already collects, "model is a Fold, form factor
 * is PHONE" becomes the used-closed segment, which nothing else can supply.
 */
enum class DeviceFormFactor {
    PHONE,
    TABLET,
    FOLDABLE_OPEN,
}

/** Whether the layout is actually showing one pane or two. */
enum class PaneMode {
    SINGLE,
    DUAL,
}

enum class ScreenOrientation {
    PORTRAIT,
    LANDSCAPE,
}

private const val TABLET_SMALLEST_WIDTH_DP = 600

/**
 * Buckets a window into the app's own device categories.
 *
 * [TABLET_SMALLEST_WIDTH_DP] is the conventional tablet threshold: below it, the shorter
 * edge is phone-shaped whichever way the device is held.
 */
object DeviceWindowClassifier {

    /**
     * A hinge inside the window means the device is open enough for the fold to matter,
     * which is the case worth separating. Everything else falls back to the shorter edge,
     * the one measure that does not flip with rotation.
     */
    fun formFactor(smallestWidthDp: Int, foldState: FoldState): DeviceFormFactor = when {
        foldState != FoldState.NONE -> DeviceFormFactor.FOLDABLE_OPEN
        smallestWidthDp >= TABLET_SMALLEST_WIDTH_DP -> DeviceFormFactor.TABLET
        else -> DeviceFormFactor.PHONE
    }

    fun orientation(widthDp: Int, heightDp: Int): ScreenOrientation =
        if (widthDp > heightDp) ScreenOrientation.LANDSCAPE else ScreenOrientation.PORTRAIT

    /**
     * True when the settled state differs from the last reported one in a way a rider
     * could act on. Pixel-level resizes and any change that leaves all three classifiers
     * where they were are not transitions and must not be sent.
     */
    fun isReportableTransition(previous: DeviceWindowSnapshot, current: DeviceWindowSnapshot): Boolean =
        previous.widthSizeClass != current.widthSizeClass ||
            previous.foldState != current.foldState ||
            previous.paneMode != current.paneMode
}

/**
 * The classified state of the window, as reported. Raw dp deliberately sits outside this
 * type: it belongs on the launch event, and including it here would make every pixel of a
 * resize animation look like a new state to compare against.
 */
data class DeviceWindowSnapshot(
    val widthSizeClass: String,
    val foldState: FoldState,
    val paneMode: PaneMode,
)
