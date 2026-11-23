package xyz.ksharma.krail.trip.planner.ui.searchstop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_map
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/** The tappable Select on map row, first under the label chips. Left out when [onOpenMap] is null, with no map to go to. */
internal fun LazyListScope.selectOnMapItem(onOpenMap: (() -> Unit)?) {
    if (onOpenMap == null) return
    item(key = "select-on-map") { SelectOnMapItem(onOpenMap = onOpenMap) }
}

/**
 * Select on map as the Search Stop list always drew it: the map icon and the words in the stop rows' own type, with the
 * stop rows' own padding so the icon and the text line up with the names below. Tapping it returns to the map state.
 */
@Composable
private fun SelectOnMapItem(
    onOpenMap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = KrailTheme.dimensions
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(TripPlannerTestTags.SEARCH_STOP_SELECT_ON_MAP)
            .klickable { onOpenMap() }
            .padding(vertical = dim.spacingL, horizontal = dim.pageHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingM),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.ic_map),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color = KrailTheme.colors.onSurface),
            modifier = Modifier.size(dim.spacingXXL),
        )
        Text(
            text = "Select on map",
            color = KrailTheme.colors.onSurface,
            style = KrailTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium),
        )
    }
}
