package xyz.ksharma.krail.core.maps.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

/**
 * Pill badge shown on the journey map to communicate timetable data freshness.
 *
 * - [isStale] = false: surface background, onSurface text — subtle, data is slightly old
 * - [isStale] = true: theme colour background, white text — prominent, data is stale
 *
 * @param text The badge label to display
 * @param isStale Whether the data is considered stale (triggers colour change)
 * @param modifier Modifier to be applied to the component
 */
@Composable
fun MapTimetableDataBadge(
    text: String,
    modifier: Modifier = Modifier,
    isStale: Boolean = false,
) {
    val themeColor by LocalThemeColor.current
    val backgroundColor = if (isStale) {
        themeColor.hexToComposeColor()
    } else {
        KrailTheme.colors.surface.copy(alpha = 0.9f)
    }
    val textColor = if (isStale) {
        getForegroundColor(backgroundColor)
    } else {
        KrailTheme.colors.onSurface
    }

    Box(
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = if (isStale) {
                KrailTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
            } else {
                KrailTheme.typography.bodySmall
            },
            color = textColor,
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun MapTimetableDataBadgeRecentPreview() {
    PreviewTheme {
        MapTimetableDataBadge(text = "Updated 1 min ago", isStale = false)
    }
}

@PreviewComponent
@Composable
private fun MapTimetableDataBadgeStalePreview() {
    PreviewTheme {
        MapTimetableDataBadge(
            text = "Displaying scheduled times, not real-time data",
            isStale = true,
        )
    }
}

// endregion
