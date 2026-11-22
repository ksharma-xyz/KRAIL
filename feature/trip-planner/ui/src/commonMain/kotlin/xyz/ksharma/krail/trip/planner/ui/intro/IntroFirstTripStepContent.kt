package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.modifier.CardShape
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.trip.planner.ui.components.SavedTripCard
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroStep
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopDisplay

/**
 * Step 2 as the slot [IntroFlowScreen] takes, with [onSearchStop] filled in. One place makes it, so
 * the route, the previews and the tests draw the same step.
 */
internal fun introFirstTripStep(onSearchStop: (isStart: Boolean) -> Unit): IntroStepContent =
    { state, suggestedStops, onEvent ->
        IntroFirstTripStepContent(
            state = state,
            suggestedStops = suggestedStops,
            onEvent = onEvent,
            onSearchStop = onSearchStop,
        )
    }

/**
 * Step 2: the first trip. It is names only, so it makes no call: the suggestions are in memory and
 * a stop the rider searches for comes back from the app's own stop search.
 *
 * What it shows comes from [introTripSections]. Whether the rider is changing a trip that is already
 * chosen is the one thing it keeps, and it is saved so a turn of the screen keeps the lists open. It
 * is keyed on the picks, so a pick, from a suggestion or from the search, closes the lists and shows
 * the tile again.
 *
 * [onSearchStop] is told which end the rider wants to search for.
 */
@Composable
internal fun IntroFirstTripStepContent(
    state: IntroState,
    suggestedStops: SuggestedStops,
    onEvent: (IntroUiEvent) -> Unit,
    onSearchStop: (isStart: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    var changing by rememberSaveable(state.startStopId, state.endStopId) { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth().padding(bottom = dim.spacingXL),
        verticalArrangement = Arrangement.spacedBy(dim.spacingXL),
    ) {
        IntroHeadline(
            lines = FIRST_TRIP_HEADLINE,
            emphasisIndex = FIRST_TRIP_HEADLINE.lastIndex,
            emoji = introEmojiFor(IntroStep.DESTINATION),
            modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
        )
        introTripSections(state, changing).forEach { section ->
            IntroTripSectionContent(
                section = section,
                suggestedStops = suggestedStops,
                onChange = { changing = true },
                onPick = { role, stop ->
                    changing = false
                    onEvent(pickEvent(role, stop))
                },
                onSearchStop = onSearchStop,
                modifier = Modifier.padding(horizontal = dim.pageHorizontalPadding),
            )
        }
    }
}

@Composable
private fun IntroTripSectionContent(
    section: IntroTripSection,
    suggestedStops: SuggestedStops,
    onChange: () -> Unit,
    onPick: (IntroStopRole, SuggestedStop) -> Unit,
    onSearchStop: (isStart: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (section) {
        is IntroTripSection.Tile -> SavedTripCard(
            fromDisplay = StopDisplay(stopId = section.startId, name = section.startName),
            toDisplay = StopDisplay(stopId = section.endId, name = section.endName),
            onCardClick = onChange,
            modifier = modifier.semantics {
                contentDescription = changeTripDescription(section.startName, section.endName)
            },
        )

        is IntroTripSection.Chosen -> IntroLabelled(role = section.role, modifier = modifier) {
            IntroChosenStopRow(
                name = section.name,
                description = changeStopDescription(section.role, section.name),
                onClick = onChange,
            )
        }

        is IntroTripSection.Picking -> IntroLabelled(role = section.role, modifier = modifier) {
            val dim = KrailTheme.dimensions
            Column(verticalArrangement = Arrangement.spacedBy(dim.spacingL)) {
                suggestedStops.offeredFor(section.role, section.otherStopId).forEach { stop ->
                    IntroAnswerButton(
                        text = stop.name,
                        selected = stop.stopId == section.selectedStopId,
                        onClick = { onPick(section.role, stop) },
                        description = pickStopDescription(section.role, stop.name),
                    )
                }
                IntroSearchRow(
                    description = searchStopDescription(section.role),
                    onClick = { onSearchStop(section.role == IntroStopRole.START) },
                )
            }
        }
    }
}

/** A section's name, then what the section holds. The name is a heading, so a screen reader can jump to it. */
@Composable
private fun IntroLabelled(
    role: IntroStopRole,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM)) {
        Text(
            text = role.sectionLabel(),
            style = KrailTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/**
 * A stop that is already chosen, drawn as `SavedTripCard` draws a stop: the same shape, the same
 * theme tint, the same padding and the same text, for one stop instead of two.
 */
@Composable
private fun IntroChosenStopRow(
    name: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = dim.iconXXXL)
            .clip(CardShape)
            .background(color = themeBackgroundColor(), shape = CardShape)
            .klickable(onClick = onClick)
            .padding(horizontal = dim.spacingXL, vertical = dim.spacingXL)
            .semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = name, style = KrailTheme.typography.bodyLarge)
    }
}

@Composable
private fun IntroSearchRow(description: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        dimensions = ButtonDefaults.largeButtonSize(),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KrailTheme.dimensions.iconXXXL)
            .semantics { contentDescription = description },
    ) {
        Text(text = SEARCH_ANOTHER_STOP, textAlign = TextAlign.Center)
    }
}
