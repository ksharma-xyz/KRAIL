package xyz.ksharma.krail.trip.planner.ui.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.discover.state.DiscoverCardType
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.darken
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.taj.themeColor

@Composable
fun DiscoverChip(
    type: DiscoverCardType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = DiscoverChipDefaults.ChipHorizontalPadding,
    verticalPadding: Dp = DiscoverChipDefaults.ChipVerticalPadding,
) {
    val textColor = if (selected) {
        getForegroundColor(
            backgroundColor = if (isSystemInDarkTheme()) themeColor().darken() else themeColor(),
        )
    } else {
        KrailTheme.colors.label
    }

    // Bubble scale animation
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = if (selected) {
            // When becoming selected: quick grow then settle
            tween(
                durationMillis = 400,
                easing = androidx.compose.animation.core.FastOutSlowInEasing,
            )
        } else {
            // When becoming deselected: quick shrink
            tween(
                durationMillis = 200,
                easing = androidx.compose.animation.core.FastOutLinearInEasing,
            )
        },
        label = "bubble_scale",
    )

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = KrailTheme.typography.bodyLarge

    val chipWidth = remember(type.displayName, textStyle, density, horizontalPadding) {
        val boldTextWidth = textMeasurer.measure(
            text = type.displayName,
            style = textStyle.copy(fontWeight = FontWeight.Bold),
        ).size.width

        val normalTextWidth = textMeasurer.measure(
            text = type.displayName,
            style = textStyle.copy(fontWeight = FontWeight.Normal),
        ).size.width

        with(density) {
            (maxOf(boldTextWidth, normalTextWidth) / density.density).dp + (horizontalPadding * 4)
        }
    }

    Box(
        modifier = modifier
            .width(chipWidth)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .border(
                width = 1.dp,
                color = if (selected) Color.Transparent else themeColor(),
                shape = RoundedCornerShape(50),
            )
            .background(
                color = if (selected) if (isSystemInDarkTheme()) themeColor().darken() else themeColor() else KrailTheme.colors.discoverChipBackground,
                shape = RoundedCornerShape(50),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = type.displayName,
            style = textStyle.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
            maxLines = 1,
            color = textColor,
            modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
            modifier = Modifier.padding(8.dp),
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
            modifier = Modifier.padding(8.dp),
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
            modifier = Modifier.padding(8.dp),
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
            modifier = Modifier.padding(8.dp),
        )
    }
}

// endregion
