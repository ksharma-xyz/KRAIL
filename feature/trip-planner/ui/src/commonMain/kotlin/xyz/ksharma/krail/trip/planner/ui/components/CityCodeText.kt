package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.antonioFontFamily

@Composable
fun CityCodeText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = KrailTheme.typography.displayLarge,
        fontFamily = antonioFontFamily(),
        modifier = modifier,
    )
}

@Preview
@Composable
private fun PreviewCityCodeText() {
    PreviewTheme {
        CityCodeText(text = "SYD")
    }
}
