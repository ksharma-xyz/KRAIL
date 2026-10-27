package xyz.ksharma.krail.info.tiles.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import xyz.ksharma.krail.core.snapshot.ScreenshotTest
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.horizontalPadding
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.shape
import xyz.ksharma.krail.info.tiles.ui.InfoTileDefaults.verticalPadding
import xyz.ksharma.krail.taj.components.AiWheelMark
import xyz.ksharma.krail.taj.components.Button
import xyz.ksharma.krail.taj.components.ButtonDefaults
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.components.TextButton
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.preview.PreviewComponent
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme

object InfoTileDefaults {
    val shape: RoundedCornerShape = RoundedCornerShape(size = 12.dp)
    val horizontalPadding = 16.dp
    val verticalPadding = 16.dp
    val borderWidth = 1.dp

    // region Shadow
    val shadowRadius = 12.dp
    val shadowSpread = 2.dp
    const val SHADOW_ALPHA = 1f
    // endregion
}

@Suppress("LongParameterList")
@Composable
fun InfoTile(
    infoTileData: InfoTileData,
    modifier: Modifier = Modifier,
    initialState: InfoTileState = InfoTileState.COLLAPSED,
    onCtaClick: (InfoTileData) -> Unit,
    onDismissClick: (InfoTileData) -> Unit,
    // required for analytics only
    onTileExpand: (InfoTileData) -> Unit = {},
    style: InfoTileStyle = InfoTileStyle.Standard,
) {
    var state by rememberSaveable { mutableStateOf(initialState) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            // Shadow must be applied before clickable modifier
            .infoTileOuter(style)
            // clip shape added before clickable so that ripple indication is bounded within shape
            .clip(shape)
            .klickable(
                onClick = {
                    state = when (state) {
                        InfoTileState.COLLAPSED -> {
                            onTileExpand(infoTileData)
                            InfoTileState.EXPANDED
                        }

                        InfoTileState.EXPANDED -> InfoTileState.COLLAPSED
                    }
                },
            )
            .infoTileSurface(style)
            .padding(vertical = verticalPadding, horizontal = horizontalPadding)
            .animateContentSize()
            .semantics(mergeDescendants = true) {},
    ) {
        InfoTileTitle(title = infoTileData.title, style = style)

        if (state == InfoTileState.EXPANDED) {
            Text(
                text = infoTileData.description,
                style = KrailTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            if (infoTileData.showDismissButton || infoTileData.primaryCta != null) {
                InfoTileActions(infoTileData = infoTileData, onCtaClick = onCtaClick, onDismissClick = onDismissClick)
            }
        }
    }
}

@Composable
private fun InfoTileTitle(title: String, style: InfoTileStyle) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (style == InfoTileStyle.AiFill) {
            // The surface's own ink, not the gradient: a gradient wheel on a gradient fill vanishes.
            val ink = KrailTheme.colors.onSurface
            AiWheelMark(spinning = true, colors = listOf(ink, ink))
        }
        Text(text = title, style = KrailTheme.typography.titleMedium)
    }
}

@Composable
private fun InfoTileActions(
    infoTileData: InfoTileData,
    onCtaClick: (InfoTileData) -> Unit,
    onDismissClick: (InfoTileData) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        if (infoTileData.showDismissButton) {
            TextButton(
                onClick = { onDismissClick(infoTileData) },
                dimensions = ButtonDefaults.mediumButtonSize(),
                modifier = Modifier.padding(end = 12.dp),
            ) {
                Text(text = infoTileData.dismissCtaText)
            }
        }

        infoTileData.primaryCta?.let { cta ->
            Button(
                dimensions = ButtonDefaults.mediumButtonSize(),
                onClick = { onCtaClick(infoTileData) },
            ) {
                Text(text = cta.text)
            }
        }
    }
}

// region Previews

@ScreenshotTest
@PreviewComponent
@Composable
private fun InfoTileLightPreview() {
    // TODO: the text color in preview is not visible properly as background colors are calculates as
    //  alpha and are not concrete colors in DS: Taj
    //  Should be rendered fine though.
    //  In future, colors should be calculated and set in tokens rather than being calculated on fly.
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip) {
        InfoTile(
            infoTileData = InfoTileData(
                title = "Service update",
                description = "Planned maintenance may cause minor delays this weekend.",
                primaryCta = InfoTileCta(
                    text = "Learn more",
                    url = "https://example.com/maintenance",
                ),
                key = "unique_tile_01",
                type = InfoTileData.InfoTileType.INFO,
            ),
            onCtaClick = {},
            onDismissClick = {},
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun InfoTileExpandedNoDismissPreview() {
    PreviewTheme {
        InfoTile(
            infoTileData = InfoTileData(
                key = "expanded_tile",
                title = "Always Expanded Tile",
                description = "This tile starts expanded and has no dismiss button, perfect for persistent content.",
                primaryCta = InfoTileCta(
                    text = "Take Action",
                ),
                type = InfoTileData.InfoTileType.INFO,
                showDismissButton = false,
            ),
            initialState = InfoTileState.EXPANDED,
            onCtaClick = {},
            onDismissClick = {},
        )
    }
}

@ScreenshotTest
@PreviewComponent
@Composable
private fun InfoTileAiFillPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Metro) {
        InfoTile(
            infoTileData = InfoTileData(
                key = "ai_fill_tile",
                title = "Want a seat?",
                description = "The next M1, at 6:33am from Platform 4, has seats available and is due at " +
                    "Barangaroo at 6:35am.",
                showDismissButton = false,
            ),
            initialState = InfoTileState.EXPANDED,
            style = InfoTileStyle.AiFill,
            onCtaClick = {},
            onDismissClick = {},
        )
    }
}

// endregion
