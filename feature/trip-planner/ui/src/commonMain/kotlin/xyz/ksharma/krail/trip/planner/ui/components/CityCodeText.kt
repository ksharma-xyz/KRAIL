package xyz.ksharma.krail.trip.planner.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.ksharma.krail.taj.components.Text
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.theme.antonioTypography

@Composable
fun CityCodeText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = antonioTypography().title,
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
