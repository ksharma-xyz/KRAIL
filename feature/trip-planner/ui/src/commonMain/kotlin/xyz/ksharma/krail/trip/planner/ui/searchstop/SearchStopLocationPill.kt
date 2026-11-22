package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

private val PILL_HEIGHT = 32.dp
private val PILL_HIT_AREA = 48.dp
private val PILL_SHADOW = 4.dp

/**
 * The one location status pill (rule 24 of `docs/MAP_HOME_UX.md`), drawn only while [pill] is not
 * [LocationPill.Hidden]. It is 32 tall inside a 48 hit area, so the 8 above and below the pill are
 * the gap to the form and to the list. White, because the status pill is the other surface colour
 * beside the dark bar. Its corners are half its height at any text size.
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
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = PILL_HIT_AREA)
            .testTag(TripPlannerTestTags.SEARCH_STOP_LOCATION_PILL)
            .klickable(onClick = onClick)
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .defaultMinSize(minHeight = PILL_HEIGHT)
                .shadow(elevation = PILL_SHADOW, shape = shape)
                .clip(shape)
                .background(KrailTheme.colors.surface)
                .padding(horizontal = KrailTheme.dimensions.spacingXL),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = LOCATION_PILL_TEXT,
                style = KrailTheme.typography.bodySmall,
                color = KrailTheme.colors.onSurface,
            )
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
