package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

@Composable
fun SavedTripCard(
    fromDisplay: StopDisplay,
    toDisplay: StopDisplay,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    favouriteIconColor: Color = KrailTheme.colors.onSurface,
) {
    val dim = KrailTheme.dimensions
    val cardShape = CardShape
    val bothLabelled = fromDisplay.label != null && toDisplay.label != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(color = themeBackgroundColor(), shape = cardShape)
            .klickable(onClick = onCardClick)
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
    ) {
        // Stop content — two layouts depending on whether both stops are labelled
        if (bothLabelled) {
            BothLabelledContent(
                fromDisplay = fromDisplay,
                toDisplay = toDisplay,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dim.spacingL),
            ) {
                StopDisplayRow(display = fromDisplay)
                StopDisplayRow(display = toDisplay)
            }
        }

        // Star button
        Box(
            modifier = Modifier
                .size(dim.savedTripIconButtonSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Remove Saved Trip",
                    role = Role.Button,
                    onClick = onStarClick,
                )
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_star_filled),
                contentDescription = null,
                colorFilter = ColorFilter.tint(favouriteIconColor),
                modifier = Modifier.size(dim.iconSmall),
            )
        }
    }
}

/**
 * Side-by-side two-column layout used when both stops carry a user label.
 * Labels are the primary text; stop names render as secondary below them.
 * A thin vertical divider separates the columns.
 */
@Composable
private fun BothLabelledContent(
    fromDisplay: StopDisplay,
    toDisplay: StopDisplay,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LabelledStopColumn(display = fromDisplay, modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(dim.strokeThin)
                .fillMaxHeight()
                .background(KrailTheme.colors.outlineSubtle),
        )
        LabelledStopColumn(display = toDisplay, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LabelledStopColumn(
    display: StopDisplay,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        ) {
            display.label?.let { label ->
                stopLabelIcon(label)?.let { icon ->
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                        modifier = Modifier.size(dim.spacingXL),
                    )
                }
                Text(
                    text = label,
                    style = KrailTheme.typography.titleMedium,
                )
            }
        }
        Text(
            text = display.name,
            style = KrailTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StopDisplayRow(
    display: StopDisplay,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dim.spacingXXS),
    ) {
        display.label?.let { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
            ) {
                stopLabelIcon(label)?.let { icon ->
                    Image(
                        painter = painterResource(icon),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                        modifier = Modifier.size(dim.spacingXL),
                    )
                }
                Text(
                    text = label,
                    style = KrailTheme.typography.titleMedium,
                )
            }
        }
        Text(
            text = display.name,
            style = KrailTheme.typography.bodyMedium,
        )
    }
}

// region Previews

@PreviewComponent
@Composable
private fun PreviewSavedTripCard_Unlabelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Central Station"),
            toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station"),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSavedTripCard_BothLabelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
            toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station", label = "Work"),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSavedTripCard_OneLabelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Manly Wharf", label = "Home"),
            toDisplay = StopDisplay(stopId = "2", name = "Circular Quay Wharf"),
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSavedTripCardList() {
    PreviewTheme {
        val dim = KrailTheme.dimensions
        Column(
            modifier = Modifier.padding(dim.spacingXL),
            verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
        ) {
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = "1", name = "Edmondson Park Station"),
                toDisplay = StopDisplay(stopId = "2", name = "Harris Park Station"),
                onCardClick = {},
                onStarClick = {},
            )
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = "1", name = "Manly Wharf", label = "Home"),
                toDisplay = StopDisplay(stopId = "2", name = "Circular Quay Wharf", label = "Work"),
                onCardClick = {},
                onStarClick = {},
            )
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
                toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station"),
                onCardClick = {},
                onStarClick = {},
            )
        }
    }
}

// endregion
