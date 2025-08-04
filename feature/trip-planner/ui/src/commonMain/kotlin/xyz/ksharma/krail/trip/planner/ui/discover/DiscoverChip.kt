package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.size
import androidx.compose.ui.unit.width
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeContentColor
import xyz.ksharma.krail.taj.themeSolidBackgroundColor

@Composable
fun DiscoverChip(
    type: DiscoverCardType,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedBackgroundColor = themeSolidBackgroundColor()
    val unselectedBackgroundColor = KrailTheme.colors.discoverChipBackground

    val textColor = if (selected) {
        getForegroundColor(backgroundColor = selectedBackgroundColor)
    } else {
        KrailTheme.colors.label
    }

    val selectedAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = LinearEasing),
        label = "selected_alpha"
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = KrailTheme.typography.title

    val chipWidth = remember(type.displayName, textStyle, density) {
        // Measure the text with bold font weight
        val boldTextWidth = textMeasurer.measure(
            text = type.displayName,
            style = textStyle.copy(fontWeight = FontWeight.Bold)
        ).size.width

        // Measure the text with normal font weight
        val normalTextWidth = textMeasurer.measure(
            text = type.displayName,
            style = textStyle.copy(fontWeight = FontWeight.Normal)
        ).size.width

        // Take the maximum of the two and add padding
        with(density) {
            (maxOf(boldTextWidth, normalTextWidth) / density.density).dp + 32.dp
        }
    }

    Box(
        modifier = modifier
            .width(chipWidth)
            .clip(RoundedCornerShape(50))
            .background(color = Color.Transparent)
            .drawBehind {
                val blendedColor =
                    lerp(unselectedBackgroundColor, selectedBackgroundColor, selectedAlpha)
                drawRect(color = blendedColor)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.displayName,
            style = KrailTheme.typography.title.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = textColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

// region Previews

@Preview(group = "Discover Chip Previews - Barbie Pink", name = "Selected Light Mode")
@Composable
private fun DiscoverChipPreview_BarbiePink_Selected() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = false) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = true,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(group = "Discover Chip Previews - Barbie Pink", name = "Unselected Light Mode")
@Preview
@Composable
private fun DiscoverChipPreview_BarbiePink_Unselected() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = false) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(group = "Discover Chip Previews - Barbie Pink", name = "Selected Dark Mode")
@Preview
@Composable
private fun DiscoverChipPreview_BarbiePink_Selected_DarkMode() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = true) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = true,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Preview(group = "Discover Chip Previews - Barbie Pink", name = "Unselected Dark Mode")
@Preview
@Composable
private fun DiscoverChipPreview_BarbiePink_Unselected_DarkMode() {
    PreviewTheme(themeStyle = KrailThemeStyle.BarbiePink, darkTheme = true) {
        DiscoverChip(
            type = DiscoverCardType.Travel,
            selected = false,
            modifier = Modifier.padding(8.dp)
        )
    }
}

// endregion
