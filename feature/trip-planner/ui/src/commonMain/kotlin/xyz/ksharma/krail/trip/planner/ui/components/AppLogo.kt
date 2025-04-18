package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        val themeColor by LocalThemeColor.current

        Text(
            text = "KRAIL",
            style = KrailTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Black,
            ),
            color = themeColor.hexToComposeColor(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Ride the rail without fail",
            style = KrailTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Normal,
            ),
            color = themeColor.hexToComposeColor(),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
