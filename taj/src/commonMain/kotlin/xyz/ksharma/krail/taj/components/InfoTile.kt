package xyz.ksharma.krail.taj.components

import androidx.compose.animation.animateBounds
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.InfoTileDefaults.SHADOW_ALPHA
import xyz.ksharma.krail.taj.components.InfoTileDefaults.borderWidth
import xyz.ksharma.krail.taj.components.InfoTileDefaults.horizontalPadding
import xyz.ksharma.krail.taj.components.InfoTileDefaults.shadowRadius
import xyz.ksharma.krail.taj.components.InfoTileDefaults.shadowSpread
import xyz.ksharma.krail.taj.components.InfoTileDefaults.shape
import xyz.ksharma.krail.taj.components.InfoTileDefaults.verticalPadding
import xyz.ksharma.krail.taj.modifier.klickable
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

@Stable
data class InfoTileData(
    val title: String,

    val description: String,

    /**
     * Type of Tile which determines its priority in the list of Tiles.
     */
    val type: InfoTileType,

    val dismissCtaText: String = "Dismiss",

    /**
     * ISO-8601 formatted date string representing the end date of the info tile.
     * E.g., "2023-12-31" for December 31, 2023.
     */
    val endDate: String? = null,

    val primaryCta: InfoTileCta? = null,
) {
    enum class InfoTileType(val priority: Int) {
        INFO(priority = 1),
        APP_UPDATE(priority = 2), //  higher priority than info, but lower than alert
        CRITICAL_ALERT(9999), // highest priority, should be shown at top of list
    }
}

@Stable
data class InfoTileCta(
    val text: String,
    val url: String,
)

enum class InfoTileState {
    COLLAPSED,
    EXPANDED
}

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

@Composable
fun InfoTile(
    infoTileData: InfoTileData,
    infoTileState: InfoTileState = InfoTileState.COLLAPSED,
    modifier: Modifier = Modifier,
    onCtaClicked: (url: String) -> Unit,
    onDismissClick: (() -> Unit) = {},
) {
    var state by rememberSaveable { mutableStateOf(infoTileState) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .klickable(
                onClick = {
                    state = when (state) {
                        InfoTileState.COLLAPSED -> InfoTileState.EXPANDED
                        InfoTileState.EXPANDED -> InfoTileState.COLLAPSED
                    }
                },
            )
            .dropShadow(
                shape = shape,
                shadow = Shadow(
                    radius = shadowRadius,
                    color = themeBackgroundColor(),
                    spread = shadowSpread,
                    alpha = SHADOW_ALPHA,
                )
            )
            .border(
                width = borderWidth,
                color = themeBackgroundColor(),
                shape = shape,
            )
            .background(color = KrailTheme.colors.surface, shape = shape)
            .padding(vertical = verticalPadding, horizontal = horizontalPadding)
            .animateContentSize()
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = infoTileData.title,
            style = KrailTheme.typography.title,
        )

        if (state == InfoTileState.EXPANDED) {
            Text(
                text = infoTileData.description,
                style = KrailTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                TextButton(
                    onClick = onDismissClick,
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text(text = infoTileData.dismissCtaText)
                }

                infoTileData.primaryCta?.let { cta ->
                    Button(
                        dimensions = ButtonDefaults.mediumButtonSize(),
                        onClick = { onCtaClicked(cta.url) },
                    ) {
                        Text(text = cta.text)
                    }
                }
            }
        }
    }
}

// region Previews

@Preview(name = "InfoTile Light")
@Composable
private fun InfoTileLightPreview() {
    // TODO: the text color in preview is not visible properly as background colors are calculates as
    //  alpha and are not concrete colors in DS: Taj
    //  Should be rendered fine though.
    //  In future, colors should be calculated and set in tokens rather than being calculated on fly.
    PreviewTheme(themeStyle = KrailThemeStyle.PurpleDrip, darkTheme = false) {
        InfoTile(
            infoTileData = InfoTileData(
                title = "Service update",
                description = "Planned maintenance may cause minor delays this weekend.",
                primaryCta = InfoTileCta(
                    text = "Learn more",
                    url = "https://example.com/maintenance",
                ),
                type = InfoTileData.InfoTileType.INFO,
            ),
            onCtaClicked = {},
            onDismissClick = {},
            modifier = Modifier.systemBarsPadding(),
        )
    }
}

@Preview(name = "InfoTile Dark")
@Composable
private fun InfoTileDarkPreview() {
    PreviewTheme(themeStyle = KrailThemeStyle.Train, darkTheme = true) {
        InfoTile(
            infoTileData = InfoTileData(
                title = "Network issue resolved",
                description = "All lines are now operating on their regular schedules.",
                type = InfoTileData.InfoTileType.INFO,
            ),
            onCtaClicked = {},
            onDismissClick = {},
        )
    }
}
// endregion