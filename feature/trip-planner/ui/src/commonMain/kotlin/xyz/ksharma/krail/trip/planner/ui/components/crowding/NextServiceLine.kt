package xyz.ksharma.krail.trip.planner.ui.components.crowding

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.feature.crowding.NextServiceWording
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.info.tiles.ui.InfoTileStyle
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

private const val NEXT_SERVICE_TILE_KEY = "next-service"

/**
 * A "Want a seat?" tile that opens to "The next T1, at 8:59am from Platform 1, has seats
 * available and is due at Central at 9:26am." Informs, never instructs: the service gets the verb
 * and the rider chooses. Tapping only opens and closes it. Data from a response the screen
 * already has, so it has no loading state of its own: present or absent.
 */
@Composable
internal fun NextServiceLine(
    text: String,
    modifier: Modifier = Modifier,
    initialState: InfoTileState = InfoTileState.COLLAPSED,
) {
    InfoTile(
        infoTileData = InfoTileData(
            key = NEXT_SERVICE_TILE_KEY,
            title = NextServiceWording.TITLE,
            description = text,
            showDismissButton = false,
        ),
        initialState = initialState,
        style = InfoTileStyle.AiFill,
        onCtaClick = {},
        onDismissClick = {},
        modifier = modifier.padding(vertical = KrailTheme.dimensions.spacingS),
    )
}

// region Previews

private const val PREVIEW_TEXT = "The next T1, at 8:59am from Platform 1, has seats available and is due " +
    "at Central at 9:26am."

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_NextServiceLine_Train() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train) {
        NextServiceLine(
            text = PREVIEW_TEXT,
            initialState = InfoTileState.EXPANDED,
            modifier = Modifier.padding(KrailTheme.dimensions.spacingL),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun Preview_NextServiceLine_Closed() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        NextServiceLine(
            text = PREVIEW_TEXT,
            modifier = Modifier.padding(KrailTheme.dimensions.spacingL),
        )
    }
}

// endregion
