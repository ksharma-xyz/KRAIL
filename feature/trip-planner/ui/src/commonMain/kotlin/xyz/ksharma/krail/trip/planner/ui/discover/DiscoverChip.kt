package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
fun DiscoverChip(
    type: DiscoverCardType,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {

    val backgroundColor = if (selected) themeBackgroundColor()
    else KrailTheme.colors.discoverChipBackground

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                color = backgroundColor,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.displayName,
            style = KrailTheme.typography.title,
            color = getForegroundColor(
                backgroundColor = backgroundColor,
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// region Previews

@Preview
@Composable
private fun DiscoverChipPreview_Metro_Selected() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = false) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = true,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview
@Composable
private fun DiscoverChipPreview_Metro_Unselected() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// endregion
