package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
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
            val selectedProductClasses: MutableSet<Int> = remember {
                mutableStateSetOf(TransportMode.Train().productClass)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TransportMode.values().sortedBy { it.priority }.forEach { mode ->
                    TransportModeChip(
                        transportMode = mode,
                        selected = selectedProductClasses.contains(mode.productClass),
                        onClick = {
                            if (selectedProductClasses.contains(mode.productClass)) {
                                selectedProductClasses.removeAll(setOf(mode.productClass))
                            } else {
                                selectedProductClasses.add(mode.productClass)
                            }
                        },
                    )
                }
            }
        }

        TagLineWithEmoji(
            tagline = tagline,
            emoji = "\uD83D\uDE0E",
            tagColor = style.hexToComposeColor()
        )
    }
}