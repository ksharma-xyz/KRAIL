package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import kotlinx.collections.immutable.ImmutableList
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_location_on
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.LocalTextColor
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.stopLabelIcon
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * The Home, Work and Park & Ride chips above the home search row, `docs/MAP_HOME_UX.md` rule 41.
 *
 * Whole chips, each taj's button at its chip size and at least as tall as the search bar (48 dp,
 * rule 21), a whole pill with no chevron: they are not dropdowns. A set label is taj's filled
 * [Button] on the stop label surface, and an unset one and Park & Ride are taj's [OutlinedButton]. When width is short the row wraps whole chips onto the next
 * line. Over 130 percent text it is instead one line that scrolls sideways under a fade at the
 * right edge, the one sideways row rule 4 allows here, with the first chip fully visible. The row
 * sits on the screen's own surface so the trips scrolling behind it never show between chips.
 *
 * Tap on a set label plans a trip to it and tap on an unset label starts setting it. There is no
 * long press: Manage stop labels opens from the label row on Search Stop. Nothing floats over
 * anything: the answer to each is the screen changing.
 */
@Composable
internal fun HomeChipRow(
    chips: ImmutableList<HomeChip>,
    onLabelClick: (labelKey: String) -> Unit,
    onParkRideClick: () -> Unit,
    modifier: Modifier = Modifier,
    // True on the map home: no surface behind the chips, so the map shows around and between them.
    floating: Boolean = false,
) {
    if (chips.isEmpty()) return
    val dim = KrailTheme.dimensions
    val rowModifier = modifier
        .testTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW)
        .fillMaxWidth()
        .then(if (floating) Modifier else Modifier.background(KrailTheme.colors.surface))
        .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM)
    val chipContent: @Composable () -> Unit = {
        chips.forEach { chip ->
            when (chip) {
                is HomeChip.Label -> LabelChip(
                    label = chip.label,
                    text = chip.text,
                    onClick = { onLabelClick(chip.label.label) },
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
 * Home or Work. Set is taj's filled [Button] on the stop label surface and unset is taj's
 * [OutlinedButton], so a chip reads the same here as on Search Stop.
 */
@Composable
private fun LabelChip(label: StopLabel, text: String, onClick: () -> Unit) {
    val modifier = Modifier.chipTarget(
        tag = TripPlannerTestTags.homeLabelChip(label.label),
        onClickLabel = if (label.isSet) "Plan a trip to ${label.label}" else null,
    )
    val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
    if (label.isSet) {
        Button(
            onClick = onClick,
            // The stop label surface is the same near black as the dark screen behind the chip
            // row, so in dark mode the fill alone would show as bare text. The outline token gives
            // it its edge back, drawn over the fill, and is fainter than an unset chip's ring so
            // the two still read apart. taj's Button has no border of its own.
            modifier = modifier.border(
                width = KrailTheme.dimensions.strokeThin,
                color = KrailTheme.colors.outlineSubtle,
                shape = ButtonDefaults.chipButtonSize().shape,
            ),
            colors = ButtonDefaults.buttonColors(
                customContainerColor = KrailTheme.colors.stopLabelSurface,
                customContentColor = KrailTheme.colors.onStopLabelSurface,
            ),
            dimensions = ButtonDefaults.chipButtonSize(),
        ) { ChipLabel(icon = icon, text = text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            dimensions = ButtonDefaults.chipButtonSize(),
        ) { ChipLabel(icon = icon, text = text) }
    }
}

/** The way into Park & Ride. Outlined, with the plus the New label chip on Search Stop uses. */
@Composable
private fun ParkRideChip(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.chipTarget(
            tag = TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE,
            onClickLabel = "Add $PARK_RIDE_LABEL",
        ),
        dimensions = ButtonDefaults.chipButtonSize(),
    ) { ChipLabel(icon = null, text = "+ $PARK_RIDE_LABEL") }
}

private const val PARK_RIDE_LABEL = "Park & Ride"

/** A chip's content: its icon, when it has one, and its text, in the button's own colour and type. */
@Composable
private fun ChipLabel(icon: DrawableResource?, text: String) {
    val dim = KrailTheme.dimensions
    val content = LocalTextColor.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(dim.spacingXS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(content),
                modifier = Modifier.size(dim.spacingXL),
            )
        }
        Text(text = text)
    }
}

/**
 * What a chip adds to taj's button: its test tag, the screen reader's name for what a tap does
 * where the label alone does not say it, and a height of at least the search bar's (48 dp).
 */
@Composable
private fun Modifier.chipTarget(tag: String, onClickLabel: String?): Modifier = this
    .testTag(tag)
    .heightIn(min = KrailTheme.dimensions.textFieldHeight)
    .then(
        if (onClickLabel != null) {
            Modifier.semantics { onClick(label = onClickLabel, action = null) }
        } else {
            Modifier
        },
    )
