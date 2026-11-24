package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel

/**
 * Every label the rider has, in the order the row shows them: Home, then Work, then the rider's own labels in their saved order,
 * set and unset alike. Nothing until the labels have loaded, so the row appears whole and never grows a pill later.
 *
 * Home and Work are always there once the labels are known: a label with no row (Work, deleted in Manage) still gets its pill,
 * as unset, so setting it again is one tap away.
 */
internal fun homeLabelsFor(stopLabels: List<StopLabel>): ImmutableList<StopLabel> {
    if (stopLabels.isEmpty()) return persistentListOf()
    val builtIn = StopLabel.defaults.map { default -> homeLabelFor(default.label, stopLabels) ?: default }
    val custom = stopLabels.filter { label -> builtIn.none { it.label.equals(label.label, ignoreCase = true) } }
    return (builtIn + custom).toImmutableList()
}

/** What a screen reader says for a label's pill, written into the pill's content description. */
internal fun homeLabelDescription(label: StopLabel): String =
    if (label.isSet) "${label.label}, double tap to go there" else "${label.label}, not set, double tap to choose a stop"

/** The label row's place in the list, kept the same so the row stays mounted when the list changes. */
private const val HOME_LABEL_ROW_KEY = "home_label_row"

/**
 * The label row as the first item of the Saved Trips list, above the info tiles and the Saved Trips heading, so it scrolls away with
 * the list. Nothing is added when there are no labels yet, so no empty slot is kept and the row never appears later and pushes the
 * list down.
 *
 * It goes while the cards are being reordered ([editing]), the way the pills did, since a tap on a label must not leave the mode. It
 * shrinks away and fades like the reorder hint does, so the cards under the rider's finger glide up instead of jumping.
 */
internal fun LazyListScope.homeLabelRowItem(
    stopLabels: List<StopLabel>,
    editing: Boolean,
    onLabelClick: (labelKey: String) -> Unit,
) {
    val labels = homeLabelsFor(stopLabels)
    if (labels.isEmpty()) return
    item(key = HOME_LABEL_ROW_KEY) {
        AnimatedVisibility(
            visible = !editing,
            enter = expandVertically(animationSpec = tween(durationMillis = 250)) + fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = shrinkVertically(animationSpec = tween(durationMillis = 200)) + fadeOut(animationSpec = tween(durationMillis = 150)),
        ) {
            HomeLabelRow(labels = labels, onLabelClick = onLabelClick)
        }
    }
}

/**
 * The rider's labels as one sideways row, the first item of the Saved Trips list, above the Saved Trips heading, so it scrolls
 * away with the list.
 *
 * Built as Search Stop's label row is: 16 dp at each end (`pageHorizontalPadding`), 8 dp between pills (`spacingM`), the pills
 * whole items in 48 dp touch slots, scrolling sideways when they overflow and **cut hard** at the trailing edge, which is the side
 * safe area edge because the list already stands inside it. There is no fade mask, no gradient and no edge fade anywhere on it.
 * While every pill fits, the cells are equal in width (the widest pill's); once they do not, each pill is as wide as its own name
 * and the last one in view is cut by the edge. A pill's text is one line that is never truncated or squeezed, so a long custom
 * name makes that pill wider and the row scrolls.
 *
 * A label with a stop is taj's filled [Button] in the theme colour, a label with no stop is taj's [OutlinedButton] reading
 * "+ Work", both at [ButtonDefaults.chipButtonSize], text only. A tap is [onLabelClick] with the label's key: the existing
 * `LabelChipTapEffect` plans a trip to a set label and starts setting an unset one.
 */
@Composable
internal fun HomeLabelRow(
    labels: ImmutableList<StopLabel>,
    onLabelClick: (labelKey: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    val density = LocalDensity.current
    val edgePx = with(density) { dim.pageHorizontalPadding.roundToPx() }
    val gapPx = with(density) { dim.spacingM.roundToPx() }
    BoxWithConstraints(modifier = modifier.testTag(TripPlannerTestTags.SAVED_TRIPS_LABEL_PILLS).fillMaxWidth()) {
        val viewportPx = constraints.maxWidth
        val policy = remember(viewportPx, edgePx, gapPx) { LabelCellsMeasurePolicy(viewportPx, edgePx, gapPx) }
        Layout(
            content = {
                labels.forEach { label ->
                    key(label.label) { HomeLabelPill(label = label, onClick = { onLabelClick(label.label) }) }
                }
            },
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            measurePolicy = policy,
        )
    }
}

/**
 * Lays the pills out left to right from [edgePx], [gapPx] apart, with [edgePx] after the last one, centred on the tallest.
 *
 * Each pill's own width comes from its intrinsic width, so it is measured once, and a pill is never given less than its own
 * text needs. If `count * widest + gaps + both edges` fits [viewportPx], every pill is given the widest one's width, so the
 * cells are equal. If not, every pill is given its own.
 */
private class LabelCellsMeasurePolicy(
    private val viewportPx: Int,
    private val edgePx: Int,
    private val gapPx: Int,
) : MeasurePolicy {

    override fun MeasureScope.measure(measurables: List<Measurable>, constraints: Constraints): MeasureResult {
        val naturals = measurables.map { it.maxIntrinsicWidth(Constraints.Infinity) }
        val widest = naturals.maxOrNull() ?: 0
        val gaps = gapPx * (measurables.size - 1).coerceAtLeast(0)
        val equal = measurables.isNotEmpty() && edgePx * 2 + widest * measurables.size + gaps <= viewportPx
        val placeables = measurables.mapIndexed { index, measurable ->
            val width = if (equal) widest else naturals[index]
            measurable.measure(Constraints(minWidth = width, maxWidth = Constraints.Infinity))
        }
        val height = placeables.maxOfOrNull { it.height } ?: 0
        val width = edgePx * 2 + placeables.sumOf { it.width } + gaps
        return layout(width, height) {
            var x = edgePx
            placeables.forEach { placeable ->
                placeable.placeRelative(x, (height - placeable.height) / 2)
                x += placeable.width + gapPx
            }
        }
    }
}

@Composable
private fun HomeLabelPill(label: StopLabel, onClick: () -> Unit) {
    val description = homeLabelDescription(label)
    // The tag and the description come before the size, so the node a screen reader and a test find is the whole 48 dp target
    // and not the button inside it.
    val target = Modifier
        .testTag(TripPlannerTestTags.homeLabelChip(label.label))
        .semantics {
            contentDescription = description
            role = Role.Button
        }
        .minimumInteractiveComponentSize()
    if (label.isSet) {
        Button(
            onClick = onClick,
            modifier = target,
            colors = ButtonDefaults.buttonColors(),
            dimensions = ButtonDefaults.chipButtonSize(),
        ) {
            Text(text = label.label, maxLines = 1)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = target,
            colors = ButtonDefaults.outlinedButtonColors(),
            dimensions = ButtonDefaults.chipButtonSize(),
        ) {
            Text(text = "+ ${label.label}", maxLines = 1)
        }
    }
}

// region Previews

private val previewLabels = persistentListOf(
    StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station"),
    StopLabel(emoji = "W", label = "Work"),
    StopLabel(emoji = "U", label = "Uni", stopId = "206710", stopName = "Chatswood Station"),
)

private val previewManyLabels = persistentListOf(
    StopLabel(emoji = "H", label = "Home", stopId = "200060", stopName = "Central Station"),
    StopLabel(emoji = "W", label = "Work"),
    StopLabel(emoji = "U", label = "Uni", stopId = "206710", stopName = "Chatswood Station"),
    StopLabel(emoji = "G", label = "Gym"),
    StopLabel(emoji = "P", label = "Grandma's place in Marrickville", stopId = "200100", stopName = "Marrickville Station"),
    StopLabel(emoji = "B", label = "Beach", stopId = "200090", stopName = "Bondi Junction"),
)

/** Three labels that fit, so the cells are equal, then six that overflow, so each pill is as wide as its name and the edge cuts the last. */
@ScreenshotTest
@PreviewComponent
@Composable
private fun HomeLabelRowPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(vertical = 16.dp)) {
            HomeLabelRow(labels = previewLabels, onLabelClick = {})
            HomeLabelRow(labels = previewManyLabels, onLabelClick = {})
        }
    }
}

// endregion
