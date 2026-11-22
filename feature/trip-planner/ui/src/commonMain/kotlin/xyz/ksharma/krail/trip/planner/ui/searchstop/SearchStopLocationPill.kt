package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.OutlinedButton
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDark
import xyz.ksharma.krail.trip.planner.ui.components.FloatingDarkTheme

private val PILL_SHADOW = 4.dp
private val PILL_HIT_AREA = 48.dp

/**
 * The one location status pill (rule 24 of `docs/MAP_HOME_UX.md`), drawn only while [pill] is not
 * [LocationPill.Hidden]. It is taj's own [Button] at its medium size, 32 tall, white because the status
 * pill is the other surface colour beside the dark bar, and it sits inside a 48 hit area, so the 8
 * above and below the pill are the gap to the form and to the list.
 *
 * It says what is wrong with the permission and nothing else. Tapping asks, or opens Settings
 * when the rider refused, and [onClick] does whichever [pill] names.
 */
@Composable
internal fun LocationStatusPill(
    pill: LocationPill,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pill == LocationPill.Hidden) return
    StatusPill(
        text = LOCATION_PILL_TEXT,
        tag = TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL,
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * One status pill: taj's [Button] at its medium size, 32 tall, in a 48 hit area. Any status that shares
 * the form's row, the location one and the Map offline chip on home, is this pill, so they are one
 * shape and one height ([enabled] false draws a status that is not a control).
 */
@Composable
internal fun StatusPill(
    text: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // The Box reserves the 48 so the 8 above and below the pill exist on every device, and the pill
    // asks for the 48 touch target too (the Button has no hit area of its own to widen).
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = PILL_HIT_AREA)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                customContainerColor = KrailTheme.colors.surface,
                customContentColor = KrailTheme.colors.onSurface,
            ),
            dimensions = ButtonDefaults.mediumButtonSize(),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .shadow(elevation = PILL_SHADOW, shape = ButtonDefaults.mediumButtonSize().shape),
        ) {
            Text(text = text)
        }
    }
}

/**
 * The location status as a small chip, in its own row of the group card on Search Stop's map state (rule 24).
 * It is taj's [OutlinedButton] at its small size on the card's fixed dark surface, with the fixed light
 * content for text and outline, left aligned, and inside a 48 hit area. It is alone on its row, so at large
 * text its words wrap inside it and nothing beside it is squeezed. The wording and what a tap does are the
 * pill's, and the tag is the pill's, so a screen reader and a test find the one status either way.
 *
 * The home keeps [LocationStatusPill] exactly as it was: this is Search Stop's own wrapper.
 */
@Composable
internal fun LocationStatusChip(
    pill: LocationPill,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (pill == LocationPill.Hidden) return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = PILL_HIT_AREA)
            .testTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL),
        contentAlignment = Alignment.CenterStart,
    ) {
        OutlinedButton(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                customContainerColor = Color.Transparent,
                customContentColor = FloatingDark.content,
            ),
            dimensions = ButtonDefaults.smallButtonSize(),
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Text(text = LOCATION_PILL_TEXT)
        }
    }
}

/** The pill's one line. The wording the location banner on this map always used. */
internal const val LOCATION_PILL_TEXT = "Location permission required"

@PreviewComponent
@Composable
private fun LocationStatusPillAskPreview() {
    PreviewTheme {
        LocationStatusPill(pill = LocationPill.Ask, onClick = {})
    }
}

@PreviewComponent
@Composable
private fun LocationStatusPillSettingsPreview() {
    PreviewTheme {
        LocationStatusPill(pill = LocationPill.OpenSettings, onClick = {})
    }
}

@PreviewComponent
@Composable
private fun LocationStatusChipPreview() {
    PreviewTheme {
        FloatingDarkTheme {
            Box(modifier = Modifier.background(FloatingDark.container).padding(KrailTheme.dimensions.cardHorizontalPadding)) {
                LocationStatusChip(pill = LocationPill.Ask, onClick = {})
            }
        }
    }
}
