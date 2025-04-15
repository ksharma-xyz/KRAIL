package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun IntroContentSelectTransportMode(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            var selectedModes by rememberSaveable { mutableStateOf(setOf<TransportMode>()) }

            TransportMode.values().forEach { mode ->
                TransportModeChip(
                    transportMode = mode,
                    selected = selectedModes.contains(mode),
                    onClick = {
                        selectedModes = if (selectedModes.contains(mode)) {
                            selectedModes - mode
                        } else selectedModes + mode
                    },
                    //modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83D\uDE0E",
            tagColor = style.hexToComposeColor()
        )
    }
}