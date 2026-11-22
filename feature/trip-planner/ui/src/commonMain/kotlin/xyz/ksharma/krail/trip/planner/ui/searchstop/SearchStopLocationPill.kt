package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

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

    // The Box reserves the 48 so the 8 above and below the pill exist on every device, and the pill
    // asks for the 48 touch target too (the Button has no hit area of its own to widen).
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = PILL_HIT_AREA)
            .testTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL),
        contentAlignment = Alignment.Center,
    ) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                customContainerColor = KrailTheme.colors.surface,
                customContentColor = KrailTheme.colors.onSurface,
            ),
            dimensions = ButtonDefaults.mediumButtonSize(),
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .shadow(elevation = PILL_SHADOW, shape = ButtonDefaults.mediumButtonSize().shape),
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
