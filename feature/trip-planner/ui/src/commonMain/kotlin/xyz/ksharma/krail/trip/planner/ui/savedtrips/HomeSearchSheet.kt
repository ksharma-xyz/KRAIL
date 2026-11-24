package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.modifier.sideSafeInsets
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags

private val SheetTopRadius = 36.dp

private val SheetVerticalPadding = 20.dp

/**
 * The sheet's colour: the rider's theme colour, as `main` drew the search row's container. The Home and Work pills
 * are drawn on it, so they read their own colours against this one ([homeUnsetPillColors]).
 */
@Composable
internal fun homeSheetColor(): Color {
    val themeColorHex by LocalThemeColor.current
    return remember(themeColorHex) { themeColorHex.hexToComposeColor() }
}

/**
 * The sheet at the bottom of the classic Saved Trips home, as `main` drew the search row's container: the rider's theme
 * colour, rounded on the two top corners only, full width, from the screen edge down behind the navigation bar. The
 * side safe inset is on the outer box so the colour itself does not draw behind a camera notch, a side navigation bar
 * or a status pill. It holds [content] in a column inside the page's own side inset and above the navigation bar: the
 * Home and Work pills when the labels have loaded, then the one search bar, 8 dp apart (`spacingM`, and no gap at all
 * when there are no pills).
 * Without pills the sheet is 96 dp tall (28 above the 48 dp bar, 20 below) and the navigation bar inset is on top of
 * that. With them it is 56 dp taller, and taller again for each line the pills wrap onto at large text, so the caller
 * reads its real height ([modifier] can carry an `onSizeChanged`) and never assumes one.
 *
 * The slide up and fade in is the caller's: [SavedTripsBottomSearchRow] reveals the whole sheet once the saved trips
 * have loaded, exactly as `main` revealed the search row, and takes it away with the pills in it while the cards are
 * being reordered.
 */
@Composable
internal fun HomeSearchSheet(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dim = KrailTheme.dimensions
    val sheetColor = homeSheetColor()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .sideSafeInsets(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(TripPlannerTestTags.SAVED_TRIPS_BOTTOM_SHEET)
                .background(
                    color = sheetColor,
                    shape = RoundedCornerShape(topStart = SheetTopRadius, topEnd = SheetTopRadius),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = SheetVerticalPadding + dim.spacingM,
                        bottom = SheetVerticalPadding + navBarPadding,
                        start = dim.pageHorizontalPadding,
                        end = dim.pageHorizontalPadding,
                    ),
                verticalArrangement = Arrangement.spacedBy(dim.spacingM),
                content = content,
            )
        }
    }
}
