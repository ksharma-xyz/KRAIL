package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/** The Home and Work pills for [stopLabels], in order, and none until the labels have loaded. */
internal fun labelPillsFor(stopLabels: List<StopLabel>): ImmutableList<HomeChip.Label> =
    homeChipsFor(stopLabels).filterIsInstance<HomeChip.Label>().toImmutableList()

/**
 * Home and Work as a wrapping row of whole pills, drawn inside [HomeSearchSheet] directly above the search bar, so
 * the row is as wide as the sheet's content and the sheet gives the page's side inset. A label with no stop is taj's
 * [OutlinedButton] at its chip size, reading "+ Home" ("+ Work", or "+ " and the name for a label the rider made),
 * in colours that read on the sheet ([homeUnsetPillColors]). A label with a stop is the in-app [SetLabelPill], the near
 * black one with its icon and name and its hairline, so the rider sees the pill change when a label is saved. Both
 * are the same height at every text size, so the row does not jump when a label is saved, and each has a 48 dp touch
 * target.
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
            .fillMaxWidth(),
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
            colors = homeUnsetPillColors(),
            dimensions = ButtonDefaults.chipButtonSize(),
        ) {
            Text(text = "+ ${label.label}", maxLines = 1)
        }
    }
}

/**
 * The unset pill's colours on the theme coloured sheet, passed through [OutlinedButton]'s own `colors` parameter and
 * nothing else: no fill, and the text and the outline in [getForegroundColor] of the sheet's colour, which is near
 * black on all four transport themes. taj's default outlined colours are the page's text colour, which is near black
 * in light mode and cream in dark mode, and the cream is only 2.21 to 2.55 to 1 on the Bus, Train and Ferry sheets
 * and 3.29 on Metro. This pair is 5.92 to 8.80 to 1 in both modes (`HomeLabelPillColorsTest`).
 */
@Composable
internal fun homeUnsetPillColors(): ButtonColors = ButtonDefaults.buttonColors(
    customContainerColor = Color.Transparent,
    customContentColor = getForegroundColor(homeSheetColor()),
)
