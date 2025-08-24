package xyz.ksharma.krail.taj.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.magicBorderColors
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor
import xyz.ksharma.krail.taj.themeColor

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

@Composable
fun InfoTile(
    infoTileState: InfoTileState,
    modifier: Modifier = Modifier,
    backgroundColor: Color = themeBackgroundColor(),
    onCtaClicked: (url: String) -> Unit,
    onDismissClick: (() -> Unit) = {},
) {
    val infiniteTransition = rememberInfiniteTransition()
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        themeColor(),
                        KrailTheme.colors.magicYellow,
                        themeColor(),
                    ),
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 16.dp, horizontal = 12.dp),
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