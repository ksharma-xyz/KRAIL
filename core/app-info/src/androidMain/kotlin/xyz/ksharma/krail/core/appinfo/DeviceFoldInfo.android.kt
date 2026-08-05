package xyz.ksharma.krail.core.appinfo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Reads the fold posture from `androidx.window`.
 *
 * A device with no hinge simply reports no [FoldingFeature], which stays [FoldState.NONE]
 * - the same value a non-foldable and an unknown posture produce, by design.
 */
@Composable
actual fun rememberDeviceFoldInfo(): DeviceFoldInfo {
    val context = LocalContext.current
    val foldInfo: State<DeviceFoldInfo> = produceState(
        initialValue = DeviceFoldInfo(),
        key1 = context,
    ) {
        WindowInfoTracker.getOrCreate(context)
            .windowLayoutInfo(context)
            .collect { layoutInfo ->
                val fold = layoutInfo.displayFeatures.filterIsInstance<FoldingFeature>().firstOrNull()
                value = DeviceFoldInfo(
                    state = fold.toFoldState(),
                    orientation = fold.toFoldOrientation(),
                )
            }
    }
    return foldInfo.value
}

private fun FoldingFeature?.toFoldState(): FoldState = when (this?.state) {
    FoldingFeature.State.FLAT -> FoldState.FLAT
    FoldingFeature.State.HALF_OPENED -> FoldState.HALF_OPENED
    else -> FoldState.NONE
}

private fun FoldingFeature?.toFoldOrientation(): FoldOrientation = when (this?.orientation) {
    FoldingFeature.Orientation.HORIZONTAL -> FoldOrientation.HORIZONTAL
    FoldingFeature.Orientation.VERTICAL -> FoldOrientation.VERTICAL
    else -> FoldOrientation.NONE
}
