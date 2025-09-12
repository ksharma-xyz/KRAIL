package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.LocalTextStyle
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun TransportModeBadge(
    badgeText: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalTextColor provides Color.White,
        LocalTextStyle provides KrailTheme.typography.titleMedium,
    ) {
        Box(
            modifier = modifier
                .clip(shape = RoundedCornerShape(percent = 20))
                .background(color = backgroundColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badgeText,
                color = Color.White,
                modifier = Modifier
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .wrapContentWidth(),
            )
        }
    }
}

// region Previews

@Preview
@Composable
private fun TransportModeBadgeBusPreview() {
    KrailTheme {
        TransportModeBadge(
            badgeText = "700",
            backgroundColor = "00B5EF".hexToComposeColor(),
        )
    }
}

@Composable
private fun TransportModeBadgeTrainPreview() {
    KrailTheme {
        TransportModeBadge(
            badgeText = "T1",
            backgroundColor = "#F6891F".hexToComposeColor(),
        )
    }
}

@Composable
private fun TransportModeBadgeFerryPreview() {
    KrailTheme {
        TransportModeBadge(
            badgeText = "F1",
            backgroundColor = "#5AB031".hexToComposeColor(),
        )
    }
}

// endregion
