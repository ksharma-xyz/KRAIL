package xyz.ksharma.krail.core.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Returns a new [ImageBitmap] with a KRAIL branding header painted above the original image.
 *
 * The header contains:
 *  - [titleText]    — centred, bold, titleLarge size (~22sp)
 *  - [subtitleText] — centred, caption size (~12sp)
 *  - 24 dp padding above the title and below the subtitle
 *
 * @param backgroundColor Background colour of the header strip (should match the card background).
 * @param textColor        Colour used for both title and subtitle text.
 * @param density          Screen density (from [androidx.compose.ui.platform.LocalDensity]) used
 *                         to convert dp/sp values to pixels.
 */
expect fun ImageBitmap.withBrandingHeader(
    titleText: String = "KRAIL",
    subtitleText: String = "https://krail.app",
    backgroundColor: Color,
    textColor: Color,
    density: Float,
): ImageBitmap

