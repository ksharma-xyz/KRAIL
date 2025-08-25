package xyz.ksharma.krail.taj.components

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
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

@Stable
data class InfoTileState(
    val title: String,
    val description: String,
    val dismissCtaText: String = "Dismiss",
    val primaryCta: InfoTileCta? = null,
)

@Stable
data class InfoTileCta(
    val text: String,
    val url: String,
)

object InfoTileDefaults {
    val shape: RoundedCornerShape = RoundedCornerShape(size = 12.dp)
    val horizontalPadding = 12.dp
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
    infoTileState: InfoTileState,
    modifier: Modifier = Modifier,
    onCtaClicked: (url: String) -> Unit,
    onDismissClick: (() -> Unit) = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = themeBackgroundColor(),
                shape = shape,
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
            .background(color = KrailTheme.colors.surface, shape = shape)
            .padding(vertical = verticalPadding, horizontal = horizontalPadding)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = infoTileState.title,
            style = KrailTheme.typography.title,
        )

        Text(
            text = infoTileState.description,
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
                Text(text = infoTileState.dismissCtaText)
            }

            infoTileState.primaryCta?.let { cta ->
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
            infoTileState = InfoTileState(
                title = "Service update",
                description = "Planned maintenance may cause minor delays this weekend.",
                primaryCta = InfoTileCta(
                    text = "Learn more",
                    url = "https://example.com/maintenance",
                ),
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
            infoTileState = InfoTileState(
                title = "Network issue resolved",
                description = "All lines are now operating on their regular schedules.",
            ),
            onCtaClicked = {},
            onDismissClick = {},
        )
    }
}
// endregion