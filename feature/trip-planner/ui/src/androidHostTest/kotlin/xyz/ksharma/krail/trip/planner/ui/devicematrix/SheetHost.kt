package xyz.ksharma.krail.trip.planner.ui.devicematrix

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.SheetContent
import xyz.ksharma.krail.taj.theme.KrailTheme

/**
 * A sheet body placed as `ModalBottomSheet` places it, for the device matrix. The sheet's own window cannot be shown in a host
 * test, so this is the next best thing: the body goes in [SheetContent], the one composable both platform sheets put their
 * content in, on a surface at the foot of the window that is centred and no wider than Material 3's sheet maximum
 * (`BottomSheetDefaults.SheetMaxWidth`), which is how the Material 3 sheet sizes its surface.
 *
 * The width cap matters: a sheet is full width only in a window narrower than the cap, so only there does its edge reach a
 * side strip. Left out, a test would call a sheet broken on a wide window where the sheet never touches the strip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SheetHost(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .widthIn(max = BottomSheetDefaults.SheetMaxWidth)
                .fillMaxWidth()
                .background(KrailTheme.colors.bottomSheetBackground),
        ) {
            SheetContent(content = content)
        }
    }
}
