package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import xyz.ksharma.krail.taj.modifier.cardBackground
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.isAppInDarkMode
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
    Row(
        modifier = modifier
            .cardBackground()
            .klickable(onClick = onCardClick)
            .padding(vertical = dim.spacingXL, horizontal = dim.spacingXL),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        primaryTransportMode?.let {
            TransportModeIcon(
                transportMode = primaryTransportMode,
                modifier = Modifier.padding(end = dim.cardInternalSpacing),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dim.cardInternalSpacing),
        ) {
            StopDisplayRow(display = fromDisplay)
            StopDisplayRow(display = toDisplay)
        }

        Box(
            modifier = Modifier
                .size(dim.savedTripIconButtonSize)
                .clip(CircleShape)
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
                contentDescription = "Save Trip",
                colorFilter = ColorFilter.tint(
                    if (isAppInDarkMode().not()) {
                        primaryTransportMode?.let { NswTransportConfig.colorFor(it) }
                            ?.hexToComposeColor()
                            ?: themeColor()
                    } else {
                        KrailTheme.colors.onSurface
                    },
                ),
            )
        }
    }
}

@Composable
private fun StopDisplayRow(
    display: StopDisplay,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val displayText = if (display.label != null) {
        "${display.label} (${display.name})"
    } else {
        display.name
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
    ) {
        display.label?.let { label ->
            stopLabelIcon(label)?.let { icon ->
                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(KrailTheme.colors.onSurface),
                    modifier = Modifier.size(dim.spacingL),
                )
            }
        }
        Text(text = displayText, style = KrailTheme.typography.bodyMedium)
    }
}

// region Previews

@PreviewComponent
@Composable
private fun SavedTripCardPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Edmondson Park Station"),
            toDisplay = StopDisplay(stopId = "2", name = "Harris Park Station"),
            primaryTransportMode = TransportMode.Train,
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun SavedTripCardLabelledPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        SavedTripCard(
            fromDisplay = StopDisplay(stopId = "1", name = "Central Station", label = "Home"),
            toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station", label = "Work"),
            primaryTransportMode = TransportMode.Train,
            onCardClick = {},
            onStarClick = {},
        )
    }
}

@PreviewComponent
@Composable
private fun SavedTripCardListPreview() {
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
                fromDisplay = StopDisplay(stopId = "1", name = "Harrington Street, Stand D"),
                toDisplay = StopDisplay(stopId = "2", name = "Albert Rd, Stand A"),
                primaryTransportMode = TransportMode.Bus,
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
                fromDisplay = StopDisplay(stopId = "1", name = "Central Station"),
                toDisplay = StopDisplay(stopId = "2", name = "Town Hall Station"),
                primaryTransportMode = TransportMode.Metro,
                onCardClick = {},
                onStarClick = {},
            )
        }
    }
}

// endregion
