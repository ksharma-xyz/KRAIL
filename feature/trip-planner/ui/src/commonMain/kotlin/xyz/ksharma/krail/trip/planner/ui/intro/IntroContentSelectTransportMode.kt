package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.trip.planner.ui.components.TransportModeChip
import xyz.ksharma.krail.trip.planner.ui.state.TransportMode

@Composable
fun IntroContentSelectTransportMode(
    tagline: String,
    style: String, // hexCode - // todo - see if it can be color instead.
    modifier: Modifier = Modifier,
    onInteraction: () -> Unit = {},
) {
    val allModes = TransportMode.values().sortedBy { it.priority }
    val selectedProductClasses = remember { mutableStateSetOf<String>() }

    LaunchedEffect(Unit) {
        val total = allModes.size
        var step = 0
        var forward = true
        while (true) {
            selectedProductClasses.clear()
            when {
                step == 0 -> { /* none selected */ }
                step in 1..total -> {
                    selectedProductClasses.addAll(allModes.take(step).map { it.productClass.toString() })
                }
                step == total + 1 -> { // all selected
                    selectedProductClasses.addAll(allModes.map { it.productClass.toString() })
                }
            }
            delay(1000)
            if (forward) {
                step++
                if (step > total + 1) {
                    forward = false
                    step = total + 1
                }
            } else {
                step--
                if (step < 0) {
                    forward = true
                    step = 0
                }
            }
        }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                allModes.forEach { mode ->
                    TransportModeChip(
                        transportMode = mode,
                        selected = selectedProductClasses.contains(mode.productClass.toString()),
                        onClick = {
                            onInteraction()
                            if (selectedProductClasses.contains(mode.productClass.toString())) {
                                selectedProductClasses.remove(mode.productClass.toString())
                            } else {
                                selectedProductClasses.add(mode.productClass.toString())
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
