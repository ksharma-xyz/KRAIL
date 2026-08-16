package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import krail.feature.trip_planner.ui.generated.resources.Res
import krail.feature.trip_planner.ui.generated.resources.ic_clock
import org.jetbrains.compose.resources.painterResource
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.getForegroundColor

private const val CHIP_CORNER_PERCENT = 50

/**
 * The departure or arrival time a search will run with, before any timetable exists.
 *
 * Deliberately the same pill and the same `toDateTimeText()` wording the timetable's own time
 * button uses, so the rider meets one control several times rather than several controls that
 * happen to agree. It lives here rather than inside either caller because it now appears on
 * two surfaces — the home search row, and the Ask KRAIL result card — and two copies would
 * have drifted the first time one of them was adjusted.
 *
 * @param onClick What tapping does, or `null` for a chip that only reports. The home row clears
 * the time back to now; on the Ask KRAIL result card the time is part of an answer being read
 * rather than leftover state, and a control that looks tappable and is not is worse than a
 * label.
 */
@Composable
internal fun WhenChip(text: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val dim = KrailTheme.dimensions
    // The surface the From and To pills use. As bare text on the coloured row this was white
    // and bold and read as a heading, louder than the two stops it qualifies. In a pill of its
    // own it belongs to the row instead of shouting over it.
    val chipBackground = KrailTheme.colors.surface
    // Checked against the pill it actually sits on rather than assumed, so it holds up on
    // every theme and in both light and dark.
    val chipForeground = getForegroundColor(backgroundColor = chipBackground)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(percent = CHIP_CORNER_PERCENT))
            .background(chipBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = dim.spacingL, vertical = dim.spacingS),
        horizontalArrangement = Arrangement.spacedBy(dim.spacingS),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // A clock says "this is a time" before a word of it is read, which is the whole job of
        // a chip sitting beside two stop names.
        Image(
            painter = painterResource(Res.drawable.ic_clock),
            contentDescription = null,
            colorFilter = ColorFilter.tint(chipForeground),
            modifier = Modifier.size(dim.iconSmall),
        )
        Text(
            text = text,
            // Smaller than the stop names beside it: this qualifies the search, it is not the
            // search.
            style = KrailTheme.typography.bodySmall,
            color = chipForeground,
        )
    }
}
