package xyz.ksharma.krail.taj.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeBackgroundColor

data class InfoTileCta(
    val text: String,
    val url: String,
)

@Composable
fun InfoTile(
    title: String,
    description: String,
    dismissText: String = "Dismiss",
    modifier: Modifier = Modifier,
    infoTileCta: InfoTileCta? = null,
    backgroundColor: Color = themeBackgroundColor(),
    onCtaClicked: (url: String) -> Unit,
    onDismissClick: (() -> Unit) = {},
) {
    Row(
        modifier = modifier.fillMaxWidth()
            .background(color = backgroundColor, shape = RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = KrailTheme.typography.title,
            )

            Text(
                text = description,
                style = KrailTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )

            Row(
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                TextButton(
                    onClick = onDismissClick,
                    dimensions = ButtonDefaults.mediumButtonSize(),
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    Text(text = dismissText)
                }

                infoTileCta?.let { cta ->
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
            title = "Service update",
            description = "Planned maintenance may cause minor delays this weekend.",
            infoTileCta = InfoTileCta(
                text = "Learn more",
                url = "https://example.com/maintenance"
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
            title = "Network issue resolved",
            description = "All lines are now operating on their regular schedules.",
            infoTileCta = null,
            onCtaClicked = {},
            onDismissClick = {},
        )
    }
}
// endregion