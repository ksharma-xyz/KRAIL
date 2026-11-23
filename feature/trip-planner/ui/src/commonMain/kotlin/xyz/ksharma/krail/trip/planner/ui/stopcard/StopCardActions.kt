package xyz.ksharma.krail.trip.planner.ui.stopcard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.motion.isReduceMotionEnabled
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags.StopCard
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDarkTheme
import xyz.ksharma.krail.trip.planner.ui.components.LabelPillSize
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.components.UnsetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import xyz.ksharma.krail.trip.planner.ui.state.searchstop.model.StopItem

/**
 * The actions of a stop card for [stop], in the order of `docs/MAP_HOME_UX.md` (Stop card): the Save as
 * pills first, then the one primary action, Go here or Start here (Select stop while choosing a stop for
 * a label). One block under another at every width, so nothing is squeezed, and the pills are whole items
 * that wrap. [onPick] is the host's, which puts the keyboard away, hands the stop on and closes the card.
 */
@Composable
internal fun StopCardPickActions(
    stop: StopItem,
    options: StopCardOptions,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
    reduceMotion: Boolean = isReduceMotionEnabled(),
) {
    val pills = remember(stop.stopId, options.pickRole, options.labels) {
        saveAsPillsFor(stop.stopId, options.pickRole, options.labels)
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(KrailTheme.dimensions.spacingM),
    ) {
        if (pills.isNotEmpty()) {
            SaveAsRow(pills = pills, onSave = { label -> options.onSaveAs(label, stop) })
        }
        if (offersPick(stop.stopId)) {
            StopCardAction(
                text = options.pickText ?: pickActionText(options.pickRole),
                onClick = onPick,
                modifier = Modifier.testTag(StopCard.PICK),
                singleLine = options.pickText != null,
                reduceMotion = reduceMotion,
            )
        }
    }
}

/**
 * The Save as pills, the app's own label pills: the label this stop carries is [SetLabelPill], filled and
 * not tappable, and a label no stop carries is [UnsetLabelPill], outlined, worded "Save as Home". Whole
 * items that wrap, and the row keeps the tallest room it has held ([reservedRoom]), so assigning a label,
 * which turns the row into one filled pill, never shortens the card.
 */
@Composable
private fun SaveAsRow(pills: List<SaveAsPill>, onSave: (StopLabel) -> Unit) {
    val dim = KrailTheme.dimensions
    val density = LocalDensity.current
    var reserved by remember { mutableStateOf(0.dp) }
    // The pills are the app's own and the unset one reads mode adaptive tokens with no colours to pass, so the
    // row is drawn in the dark scheme the card is.
    FloatingDarkTheme {
        FlowRow(
            modifier = Modifier
                .testTag(StopCard.SAVE_AS)
                .fillMaxWidth()
                .heightIn(min = reserved)
                .onSizeChanged { size -> reserved = reservedRoom(reserved, with(density) { size.height.toDp() }) },
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalArrangement = Arrangement.spacedBy(dim.spacingS),
        ) {
            pills.forEach { pill ->
                key(pill.label.label) {
                    when (pill) {
                        is SaveAsPill.Saved -> SavedPill(pill.label)
                        is SaveAsPill.Offered -> OfferedPill(pill = pill, onSave = { onSave(pill.label) })
                    }
                }
            }
        }
    }
}

/** The label this stop carries. A status, so it is not a control and a screen reader hears it as one. */
@Composable
private fun SavedPill(label: StopLabel) {
    Box(
        modifier = Modifier
            .testTag(StopCard.saveAsPill(label.label))
            .heightIn(min = KrailTheme.dimensions.textFieldHeight)
            .clearAndSetSemantics { contentDescription = "Saved as ${label.label}" },
        contentAlignment = Alignment.Center,
    ) {
        SetLabelPill(label = label, size = LabelPillSize.Normal, showIcon = false)
    }
}

/** A label no stop carries: a tap gives it to this stop. The target is as tall as the search bar, 48 dp. */
@Composable
private fun OfferedPill(pill: SaveAsPill.Offered, onSave: () -> Unit) {
    val dim = KrailTheme.dimensions
    Box(
        modifier = Modifier
            .testTag(StopCard.saveAsPill(pill.label.label))
            .heightIn(min = dim.textFieldHeight)
            .clip(RoundedCornerShape(dim.radiusFull))
            .klickable(onClick = onSave),
        contentAlignment = Alignment.Center,
    ) {
        UnsetLabelPill(label = pill.label, showIcon = false, text = pill.text)
    }
}

/**
 * The secondary action of a card: taj's [OutlinedButton] at the large size, so it stands beside the
 * primary [StopCardAction] in the same width. Used where a card has two whole actions.
 */
@Composable
internal fun StopCardSecondaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = KrailTheme.dimensions.textFieldHeight),
        colors = FloatingDark.outlinedButtonColors(),
        dimensions = ButtonDefaults.largeButtonSize(),
    ) {
        Text(text = text)
    }
}
