package xyz.ksharma.krail.park.ride.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import krail.feature.park_ride.ui.generated.resources.Res
import krail.feature.park_ride.ui.generated.resources.ic_car
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

@Composable
fun ParkAndRideIcon() {
    var themeColor by LocalThemeColor.current

    Box(
        modifier = Modifier
            .size(height = 44.dp, width = 32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color = Color.White),
    ) {
        Text(
            text = "P",
            style = KrailTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = themeColor.hexToComposeColor(),
            modifier = Modifier.padding(start = 4.dp, top = 4.dp),
        )

        Image(
            painter = painterResource(Res.drawable.ic_car),
            contentDescription = null,
            colorFilter = ColorFilter.tint(themeColor.hexToComposeColor()),
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.Center)
                .padding(start = 8.dp, top = 8.dp),
        )
    }
}

@Preview
@Composable
private fun ParkAndRideIconPreview() {
    PreviewTheme {
        ParkAndRideIcon()
    }
}

@Preview
@Composable
private fun ParkAndRideIconThemedPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        ParkAndRideIcon()
    }
}
