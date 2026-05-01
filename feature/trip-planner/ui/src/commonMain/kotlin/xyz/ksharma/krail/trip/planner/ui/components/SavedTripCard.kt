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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_star_filled
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.core.transport.nsw.NswTransportConfig
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

@Composable
fun SavedTripCard(
    fromDisplay: StopDisplay,
    toDisplay: StopDisplay,
    primaryTransportMode: TransportMode?,
    onStarClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val cardShape = CardShape
    val themeColor = themeColor()
    val modeHexColor = primaryTransportMode?.let { NswTransportConfig.colorFor(it) }
    val starColor = if (isAppInDarkMode().not()) {
        modeHexColor?.hexToComposeColor() ?: themeColor
    } else {
        KrailTheme.colors.onSurface
    }

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
        // Stop names
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dim.spacingL),
        ) {
            StopDisplayRow(display = fromDisplay)
            StopDisplayRow(display = toDisplay)
        }

        // Right side: mode pill (if available) + star
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxHeight(),
        ) {
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
                    colorFilter = ColorFilter.tint(starColor),
                    modifier = Modifier.size(dim.iconSmall),
                )
            }
        }
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
                Text(
                    text = label,
                    style = KrailTheme.typography.titleMedium,
                )
            }
        }
        Text(
            text = display.name,
            style =  KrailTheme.typography.bodyMedium,
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
            primaryTransportMode = TransportMode.Train,
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSavedTripCard_Labelled() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
            toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station", label = "Work"),
            primaryTransportMode = TransportMode.Bus,
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun PreviewSavedTripCard_NoMode() {
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Manly Wharf", label = "Home"),
            toDisplay = StopDisplay(stopId = "2", name = "Circular Quay Wharf"),
            primaryTransportMode = null,
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
                primaryTransportMode = TransportMode.Train,
                onCardClick = {},
                onStarClick = {},
            )
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = "1", name = "Manly Wharf", label = "Home"),
                toDisplay = StopDisplay(stopId = "2", name = "Circular Quay Wharf", label = "Work"),
                primaryTransportMode = TransportMode.Ferry,
                onCardClick = {},
                onStarClick = {},
            )
            SavedTripCard(
                fromDisplay = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
                toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station", label = "Work"),
                primaryTransportMode = TransportMode.Metro,
                onCardClick = {},
                onStarClick = {},
            )
        }
    }
}

// endregion
