package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.taj.components.NavActionButton
import xyz.ksharma.krail.taj.theme.KrailTheme

internal const val BACK_DESCRIPTION = "Back"

/**
 * The bar above the screen: the app's own back button, top left, when there is a screen to go back
 * to. The room for it is reserved on every screen, so nothing moves when it appears. There is
 * nothing else in the bar, and in particular no progress marker.
 */
@Composable
internal fun IntroTopBar(showBack: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dim.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(dim.iconXXXL), contentAlignment = Alignment.Center) {
            if (showBack) {
                NavActionButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    iconContentDescription = BACK_DESCRIPTION,
                    onClick = onBack,
                )
            }
        }
    }
}
