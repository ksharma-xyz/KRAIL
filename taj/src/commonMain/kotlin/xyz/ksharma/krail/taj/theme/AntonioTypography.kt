package xyz.ksharma.krail.taj.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.krail.taj.resources.Res
import app.krail.taj.resources.antonio_regular
import app.krail.taj.resources.antonio_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun antonioFontFamily() = FontFamily(
    Font(Res.font.antonio_regular, FontWeight.Normal),
    Font(Res.font.antonio_semibold, FontWeight.SemiBold),
)

@Composable
fun antonioTypography(): KrailTypography {

    return with(KrailTheme.typography) {
        copy(
            body = body.copy(fontFamily = antonioFontFamily(), fontWeight = FontWeight.Normal),
            title = title.copy(fontFamily = antonioFontFamily(), fontWeight = FontWeight.Bold),
            bodyLarge = bodyLarge.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
            bodyMedium = bodyMedium.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            bodySmall = bodySmall.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
            displayLarge = displayLarge.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold,
            ),
            displayMedium = displayMedium.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            displaySmall = displaySmall.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            headlineLarge = headlineLarge.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            headlineMedium = headlineMedium.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            headlineSmall = headlineSmall.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            labelLarge = labelLarge.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
            labelMedium = labelMedium.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
            labelSmall = labelSmall.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
            titleLarge = titleLarge.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            titleMedium = titleMedium.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold
            ),
            titleSmall = titleSmall.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Bold,
            ),
            emoji = emoji.copy(fontFamily = antonioFontFamily(), fontWeight = FontWeight.Bold),
            introTagline = introTagline.copy(
                fontFamily = antonioFontFamily(),
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}