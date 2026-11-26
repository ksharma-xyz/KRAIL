package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.persistentSetOf
import xyz.ksharma.krail.core.maps.data.location.rememberUserLocationManager
import xyz.ksharma.krail.core.transport.TransportMode
import xyz.ksharma.krail.taj.preview.PreviewScreen
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.components.ToFromField
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

// Previews of the Search Stop box with its location chip and of the mode button and block, on the flat ground the
// cards float on.

private const val PREVIEW_TOWN_HALL = "Town Hall"
private const val PREVIEW_TOWN_HALL_ID = "stop_town_hall"

/** A form holding a destination, with the query field on the From bar, for the group card previews. */
@Composable
private fun previewFormModel(): SearchStopFormModel = SearchStopFormModel(
    shape = FormShape.ToAndFrom,
    editing = ToFromField.To,
    toPlaceholder = "Choose destination",
    fromPlaceholder = "Choose starting point",
    toStop = StopItem(stopId = PREVIEW_TOWN_HALL_ID, stopName = PREVIEW_TOWN_HALL),
    fromStop = null,
    fieldState = rememberTextFieldState(),
    focusRequester = remember { FocusRequester() },
    onFlowEvent = {},
    onGoClick = {},
    onTextChange = {},
)

@Composable
private fun previewLocationState(pill: LocationPill): LocationPillState = LocationPillState(
    pill = pill,
    granted = pill == LocationPill.Hidden,
    ask = LocationPermissionAsk(request = null, onStatus = {}, onRequest = {}),
    userLocationManager = rememberUserLocationManager(),
    onClick = {},
)

/** The flat stand-in for the always light ground the cards float on. */
@Composable
private fun PreviewGround(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(PREVIEW_GROUND))
            .padding(KrailTheme.dimensions.spacingXL),
    ) { content() }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopGroupCard_WithStatusChip() {
    // The box, then the location status chip as its own small button under it at the box's left edge.
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        PreviewGround {
            SearchStopTopBlock(
                form = previewFormModel(),
                location = previewLocationState(LocationPill.Ask),
                onBack = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopGroupCard_NoStatusChip() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        PreviewGround {
            SearchStopTopBlock(
                form = previewFormModel(),
                location = previewLocationState(LocationPill.Hidden),
                onBack = {},
            )
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopModeBlock_Closed() {
    // Every mode on, so the button has no active look.
    PreviewTheme(themeStyle = KrailThemeStyle.Bus) {
        PreviewGround {
            SearchStopModeButton(expanded = false, filtered = false, onClick = {})
        }
    }
}

@PreviewScreen
@Composable
private fun PreviewSearchStopModeBlock_Open() {
    // Two modes switched off, so the Options button shows its active look, with the card open above it.
    PreviewTheme(themeStyle = KrailThemeStyle.Ferry) {
        PreviewGround {
            Column(verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM)) {
                SearchStopModeBlock(
                    selectedModes = persistentSetOf(TransportMode.Train.productClass, TransportMode.Metro.productClass),
                    onToggle = {},
                )
                SearchStopModeButton(expanded = true, filtered = true, onClick = {})
            }
        }
    }
}

private const val PREVIEW_GROUND = 0xFFE8E6E1
