package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import kotlinx.collections.immutable.ImmutableList
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.krailRipple
import xyz.ksharma.krail.taj.themeDecorColor
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.SetLabelPill
import xyz.ksharma.krail.trip.planner.ui.components.UnsetLabelPill
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * The Home, Work and Park & Ride chips above the home search row, `docs/MAP_HOME_UX.md` rule 41.
 *
 * Whole chips, each at least as tall as the search bar (48 dp, rule 21) and a whole pill with no
 * chevron: they are not dropdowns. When width is short the row wraps whole chips onto the next
 * line. Over 130 percent text it is instead one line that scrolls sideways under a fade at the
 * right edge, the one sideways row rule 4 allows here, with the first chip fully visible. The row
 * sits on the screen's own surface so the trips scrolling behind it never show between chips.
 *
 * Tap on a set label plans a trip to it, tap on an unset label starts setting it, and a long press
 * on a set label opens Manage stop labels. Nothing floats over anything: the answer to each is
 * the screen changing.
 */
@Composable
internal fun HomeChipRow(
    chips: ImmutableList<HomeChip>,
    onLabelClick: (labelKey: String) -> Unit,
    onLabelLongClick: () -> Unit,
    onParkRideClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (chips.isEmpty()) return
    val dim = KrailTheme.dimensions
    val rowModifier = modifier
        .testTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW)
        .fillMaxWidth()
        .background(KrailTheme.colors.surface)
        .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM)
    val chipContent: @Composable () -> Unit = {
        chips.forEach { chip ->
            when (chip) {
                is HomeChip.Label -> LabelChip(
                    label = chip.label,
                    text = chip.text,
                    onClick = { onLabelClick(chip.label.label) },
                    onLongClick = onLabelLongClick,
                )
                HomeChip.ParkRide -> ParkRideChip(onClick = onParkRideClick)
            }
        }
    }
    when (chipRowModeFor(LocalDensity.current.fontScale)) {
        ChipRowMode.Wrap -> FlowRow(
            modifier = rowModifier,
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
            verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) { chipContent() }

        ChipRowMode.Scroll -> ScrollingChips(modifier = rowModifier, content = chipContent)
    }
}

/** One line of chips that scrolls sideways, with a fade at the right edge while there is more to see. */
@Composable
private fun ScrollingChips(modifier: Modifier, content: @Composable () -> Unit) {
    val dim = KrailTheme.dimensions
    val scroll = rememberScrollState()
    Box(modifier = modifier) {
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        ) { content() }
        if (scroll.canScrollForward) {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(dim.spacingXXXXL)
                        .background(
                            Brush.horizontalGradient(listOf(Color.Transparent, KrailTheme.colors.surface)),
                        ),
                )
            }
        }
    }
}

/**
 * Home or Work. Set is the app's solid label pill and unset is its outlined one, so a chip reads the
 * same here as on Search Stop. A long press only means something on a set label.
 */
@Composable
private fun LabelChip(
    label: StopLabel,
    text: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val modifier = Modifier.chipTap(
        tag = TripPlannerTestTags.homeLabelChip(label.label),
        onClickLabel = if (label.isSet) "Plan a trip to ${label.label}" else null,
        onLongClickLabel = if (label.isSet) "Manage stop labels" else null,
        onLongClick = if (label.isSet) onLongClick else null,
        onClick = onClick,
    )
    if (label.isSet) {
        // The solid label pill is the same near black as the dark screen behind the chip row, so in
        // dark mode it would show as bare text. The outline token gives it its edge back, drawn over
        // the fill, and is fainter than an unset chip's ring so the two still read apart.
        Box(
            modifier = Modifier.border(
                width = KrailTheme.dimensions.strokeThin,
                color = KrailTheme.colors.outlineSubtle,
                shape = RoundedCornerShape(KrailTheme.dimensions.radiusFull),
            ),
        ) {
            SetLabelPill(label = label, modifier = modifier)
        }
    } else {
        UnsetLabelPill(label = label, text = text, modifier = modifier)
    }
}

/** The way into Park & Ride. Outlined, with the plus the New label chip on Search Stop uses. */
@Composable
private fun ParkRideChip(onClick: () -> Unit) {
    UnsetLabelPill(
        label = StopLabel(emoji = "", label = PARK_RIDE_LABEL),
        text = "+ $PARK_RIDE_LABEL",
        showIcon = false,
        modifier = Modifier.chipTap(
            tag = TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE,
            onClickLabel = "Add $PARK_RIDE_LABEL",
            onClick = onClick,
        ),
    )
}

private const val PARK_RIDE_LABEL = "Park & Ride"

/**
 * A whole chip's touch target: at least the search bar's height, clipped to the pill so the
 * ripple stays inside it, tap and, where there is one, long press.
 */
@Composable
private fun Modifier.chipTap(
    tag: String,
    onClick: () -> Unit,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
): Modifier {
    val dim = KrailTheme.dimensions
    val haptic = LocalHapticFeedback.current
    return this
        .testTag(tag)
        .heightIn(min = dim.textFieldHeight)
        .clip(RoundedCornerShape(dim.radiusFull))
        .combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = krailRipple(color = themeDecorColor()),
            role = Role.Button,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongClick?.let { longClick ->
                {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    longClick()
                }
            },
            onClick = onClick,
        )
}
