package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeDecorColor

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // Decor, not ink: the wordmark is the rider's chosen colour exactly, on every
        // screen that draws it. Adapting it for contrast is what made the title bar and
        // this logo disagree.
        val wordmark = themeDecorColor()

        Text(
            text = "KRAIL",
            style = KrailTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
            ),
            color = wordmark,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Ride the rail without fail",
            style = KrailTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = wordmark,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
