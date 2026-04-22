package xyz.ksharma.krail.feature.track.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.feature.track.TrackedJourney
import xyz.ksharma.krail.taj.components.RoundIconButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme

@Composable
fun TrackingCard(
    tracked: TrackedJourney,
    onCardClick: () -> Unit,
    onStopTracking: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .cardBackground()
            .klickable(onClick = onCardClick)
            .padding(vertical = 16.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = tracked.deepLink.fromStopName, style = KrailTheme.typography.bodyMedium)
            Text(text = tracked.deepLink.toStopName, style = KrailTheme.typography.bodyMedium)
        }

        RoundIconButton(
            onClick = onStopTracking,
            color = KrailTheme.colors.onSurface,
            onClickLabel = "Stop tracking",
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = KrailTheme.colors.surface,
            )
        }
    }
}
