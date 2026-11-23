package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/** The Home and Work pills for [stopLabels], in order, and none until the labels have loaded. */
internal fun labelPillsFor(stopLabels: List<StopLabel>): ImmutableList<HomeChip.Label> =
    homeChipsFor(stopLabels).filterIsInstance<HomeChip.Label>().toImmutableList()

/**
 * The first row of the Saved Trips list, directly under the title bar. It scrolls away with the list, and it is
 * gone while the cards are being reordered, so a tap cannot leave that mode.
 */
internal fun LazyListScope.labelPillsItem(
    stopLabels: List<StopLabel>,
    visible: Boolean,
    onLabelClick: (labelKey: String) -> Unit,
) {
    val pills = labelPillsFor(stopLabels)
    if (pills.isEmpty()) return
    item(key = "label_pills") {
        AnimatedVisibility(
            visible = visible,
            enter = expandVertically(animationSpec = tween(durationMillis = 250)) +
                fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) +
                fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            HomeLabelPills(pills = pills, onLabelClick = onLabelClick)
        }
    }
}

/**
 * Home and Work as a wrapping row of whole pills. A label with no stop is taj's [OutlinedButton] at its chip size,
 * in taj's own default colours, reading "+ Home" ("+ Work", or "+ " and the name for a label the rider made). A
 * label with a stop is the in-app [SetLabelPill], the near black one with its icon and name, so the rider sees
 * the pill change when a label is saved. Both are the same height at every text size, so the row does not
 * jump when a label is saved, and each has a 48 dp touch target.
 *
 * Pills are whole items in a [FlowRow], 8 dp apart: when the line is full the next pill goes onto the next line.
 * Nothing scrolls sideways and nothing is shrunk or clipped. A pill's text is one line, and if one pill alone is
 * wider than the row it ends in an ellipsis. A set label plans a trip to it and an unset one starts setting it, as
 * the chips always did.
 */
@Composable
internal fun HomeLabelPills(
    pills: ImmutableList<HomeChip.Label>,
    onLabelClick: (labelKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = modifier
            .testTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS)
            .fillMaxWidth()
            .padding(horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        pills.forEach { pill ->
            key(pill.label.label) {
                HomeLabelPill(label = pill.label, onClick = { onLabelClick(pill.label.label) })
            }
        }
    }
}

@Composable
private fun HomeLabelPill(label: StopLabel, onClick: () -> Unit) {
    val tag = TripPlannerTestTags.homeLabelChip(label.label)
    if (label.isSet) {
        // Clip before the click so the ripple stays inside the pill. The target modifier comes first, so the 48 dp
        // is the touch area around the pill and the pill itself is drawn at its own size.
        SetLabelPill(
            label = label,
            modifier = Modifier
                .chipTarget(tag = tag, onClickLabel = "Plan a trip to ${label.label}")
                .clip(RoundedCornerShape(KrailTheme.dimensions.radiusFull))
                .klickable(onClick = onClick),
            maxLines = 1,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.chipTarget(tag = tag, onClickLabel = "Set ${label.label}"),
            dimensions = ButtonDefaults.chipButtonSize(),
        ) {
            Text(text = "+ ${label.label}", maxLines = 1)
        }
    }
}
