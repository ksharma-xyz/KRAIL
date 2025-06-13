package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeColor

@Composable
fun ParkRideIcon(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(height = 44.dp, width = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color = themeColor())
            .clearAndSetSemantics {},
    ) {
        Text(
            text = "P",
            style = KrailTheme.typography.displayLarge,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp),
        )

        Image(
            painter = painterResource(Res.drawable.ic_car),
            colorFilter = ColorFilter.tint(color = Color.White),
            contentDescription = null,
            modifier = Modifier
                .size(16.dp)
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp),
        )
    }
}

@Preview
@Composable
fun ParkRideIconPreview() {
    KrailTheme {
        ParkRideIcon()
    }
}