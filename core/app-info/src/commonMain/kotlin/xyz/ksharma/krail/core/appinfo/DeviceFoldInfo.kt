package xyz.ksharma.krail.core.appinfo

import androidx.compose.runtime.Composable

/**
 * How far the device is folded, when the platform can tell us at all.
 *
 * [NONE] means "not applicable": not a foldable, or a platform with no fold API. It is
 * reported rather than omitted, because a parameter that is sometimes absent is harder to
 * reason about downstream than one that is always present with a known value.
 */
enum class FoldState {
    NONE,
    FLAT,
    HALF_OPENED,
}

/** Which way the fold runs: book-style ([VERTICAL]) or flip-style ([HORIZONTAL]). */
enum class FoldOrientation {
    NONE,
    HORIZONTAL,
    VERTICAL,
}

/** Fold posture of the window the app is currently in. */
data class DeviceFoldInfo(
    val state: FoldState = FoldState.NONE,
    val orientation: FoldOrientation = FoldOrientation.NONE,
)

/**
 * Current fold posture, recomposing as the device is folded or unfolded.
 *
 * Android reads it from `androidx.window`; every other platform reports
 * [FoldState.NONE] because no fold API exists there.
 */
@Composable
expect fun rememberDeviceFoldInfo(): DeviceFoldInfo
