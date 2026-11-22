package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
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
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.stopLabelIcon
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * The Home, Work and Park & Ride chips above the home search row, `docs/MAP_HOME_UX.md` rule 41.
 *
 * Every chip is taj's filled [Button] as it ships, at [ButtonDefaults.smallButtonSize], in the one fixed dark
 * ([FloatingDark.buttonColors]): a set label, an unset label and Park & Ride look the same, and read on the
 * light map in both app modes. The 48 dp touch target is [minimumInteractiveComponentSize], not drawn.
 *
 * The chips are whole items in a [FlowRow] at every width and text size: when the line is full the next chip
 * goes onto the next line, and nothing is shrunk, clipped or scrolled. [trailing] adds whole items to the same
 * flow (the notices on the map home), so they wrap with the chips.
 *
 * Tap on a set label plans a trip to it and tap on an unset label starts setting it. There is no
 * long press: Manage stop labels opens from the label row on Search Stop.
 */
@Composable
internal fun HomeChipRow(
    chips: ImmutableList<HomeChip>,
    onLabelClick: (labelKey: String) -> Unit,
    onParkRideClick: () -> Unit,
    modifier: Modifier = Modifier,
    // True on the map home: the caller gives the edge inset and sits the row on the map, with no surface behind.
    floating: Boolean = false,
    trailing: @Composable FlowRowScope.() -> Unit = {},
) {
    val dim = KrailTheme.dimensions
    FlowRow(
        modifier = modifier
            .testTag(TripPlannerTestTags.SAVED_TRIPS_CHIP_ROW)
            .fillMaxWidth()
            .then(
                if (floating) {
                    Modifier
                } else {
                    Modifier
                        .background(KrailTheme.colors.surface)
                        .padding(horizontal = dim.pageHorizontalPadding, vertical = dim.spacingM)
                },
            ),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalArrangement = Arrangement.spacedBy(dim.spacingM),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
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
        trailing()
    }
}

/**
 * Home or Work, set or not: the one filled taj [Button]. An unset label reads "Set Work" and is the same chip.
 * The map home's chips are in the one fixed dark ([FloatingDark.buttonColors]), and the classic home's pills pass
 * taj's own default [colors].
 */
@Composable
internal fun LabelChip(
    label: StopLabel,
    text: String,
    onClick: () -> Unit,
    colors: ButtonColors = FloatingDark.buttonColors(),
) {
    val icon = stopLabelIcon(label.label) ?: Res.drawable.ic_location_on
    Button(
        onClick = onClick,
        modifier = Modifier.chipTarget(
            tag = TripPlannerTestTags.homeLabelChip(label.label),
            onClickLabel = if (label.isSet) "Plan a trip to ${label.label}" else null,
        ),
        colors = colors,
        dimensions = ButtonDefaults.smallButtonSize(),
    ) { ChipLabel(icon = icon, text = text) }
}

/** The way into Park & Ride, the same chip, with the plus the New label chip on Search Stop uses. */
@Composable
private fun ParkRideChip(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.chipTarget(
            tag = TripPlannerTestTags.SAVED_TRIPS_CHIP_PARK_RIDE,
            onClickLabel = "Add $PARK_RIDE_LABEL",
        ),
        colors = FloatingDark.buttonColors(),
        dimensions = ButtonDefaults.smallButtonSize(),
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
                modifier = Modifier.size(dim.iconSmall),
            )
        }
        Text(text = text)
    }
}

/**
 * What a chip adds to taj's button: its test tag, the screen reader's name for what a tap does where the
 * label alone does not say it, and the 48 dp touch target from the interactive size modifier.
 */
@Composable
private fun Modifier.chipTarget(tag: String, onClickLabel: String?): Modifier = this
    // The tag and the semantics come before the size, so the node a screen reader and a test find is the whole
    // 48 dp target and not the button inside it.
    .testTag(tag)
    .then(
        if (onClickLabel != null) {
            Modifier.semantics { onClick(label = onClickLabel, action = null) }
        } else {
            Modifier
        },
    )
    .minimumInteractiveComponentSize()
