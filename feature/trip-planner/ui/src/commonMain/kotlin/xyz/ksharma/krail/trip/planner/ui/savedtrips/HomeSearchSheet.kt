package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

/** The sheet's rounded top corners, the radius `main` gave the search row. */
private val SheetTopRadius = 36.dp

/** The sheet's own vertical padding, the one `main` gave the search row: 20 dp above and below, and 8 dp more above. */
private val SheetVerticalPadding = 20.dp

/**
 * The sheet at the bottom of the classic Saved Trips home, as `main` drew the search row's container: the rider's theme
 * colour, rounded on the two top corners only, full width, from the screen edge down behind the navigation bar. The
 * cutout inset is on the outer box so the colour itself does not draw behind a camera notch in landscape. It holds
 * [content], the one search bar, inside the page's own side inset and above the navigation bar. With the 48 dp bar the
 * sheet is 96 dp tall (28 above the bar, 20 below) and the navigation bar inset is on top of that.
 *
 * The slide up and fade in is the caller's: [SavedTripsBottomSearchRow] reveals the whole sheet once the saved trips
 * have loaded, exactly as `main` revealed the search row.
 */
@Composable
internal fun HomeSearchSheet(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val dim = KrailTheme.dimensions
    val themeColorHex by LocalThemeColor.current
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
                .background(
                    color = themeColorHex.hexToComposeColor(),
                    shape = RoundedCornerShape(topStart = SheetTopRadius, topEnd = SheetTopRadius),
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SheetVerticalPadding + dim.spacingM,
                        bottom = SheetVerticalPadding + navBarPadding,
                        start = dim.pageHorizontalPadding,
                        end = dim.pageHorizontalPadding,
                    ),
            ) {
                content()
            }
        }
    }
}
