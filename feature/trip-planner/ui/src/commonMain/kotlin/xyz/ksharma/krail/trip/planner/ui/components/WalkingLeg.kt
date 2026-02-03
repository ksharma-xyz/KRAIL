package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_walk
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalContentAlpha
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun WalkingLeg(
    duration: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    // todo can be reusable logic for consistent icon size
    val iconSize = with(density) { 18.sp.toDp() }
    val contentAlpha = LocalContentAlpha.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = KrailTheme.colors.surface),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_walk),
            contentDescription = null,
            colorFilter = ColorFilter.tint(
                color = KrailTheme.colors.onSurface.copy(alpha = contentAlpha),
            ),
            modifier = Modifier.size(iconSize),
        )
        Text("Walk $duration", style = KrailTheme.typography.bodyLarge)
    }
}

@Preview
@Composable
private fun PreviewWalkingLeg() {
    PreviewTheme {
        WalkingLeg("5 mins")
    }
}
