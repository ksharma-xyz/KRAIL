package xyz.ksharma.krail.core.share

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontMgr
import org.jetbrains.skia.FontStyle
import org.jetbrains.skia.Image
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Rect
import org.jetbrains.skia.Surface
import org.jetbrains.skia.TextLine
import org.jetbrains.skia.Typeface

// ARGB channel bit-shift positions and max channel value for Skia colour packing
private const val ARGB_MAX_CHANNEL = 255
private const val ARGB_ALPHA_SHIFT = 24
private const val ARGB_RED_SHIFT = 16
private const val ARGB_GREEN_SHIFT = 8
private const val ARGB_BLUE_SHIFT = 0

actual fun ImageBitmap.withBrandingHeader(
    titleText: String,
    subtitleText: String,
    backgroundColor: Color,
    textColor: Color,
    density: Float,
): ImageBitmap {
    val originalBitmap = asSkiaBitmap()
    val width = originalBitmap.width
    val originalHeight = originalBitmap.height

    val titleSizePx = BRANDING_TITLE_SIZE_SP * density
    val subtitleSizePx = BRANDING_SUBTITLE_SIZE_SP * density
    val paddingPx = BRANDING_HEADER_PADDING_DP * density
    val gapPx = BRANDING_TITLE_SUBTITLE_GAP_DP * density

    val titleBaseline = paddingPx + titleSizePx
    val subtitleBaseline = titleBaseline + gapPx + subtitleSizePx
    val headerHeight = (subtitleBaseline + paddingPx).toInt()

    val totalHeight = originalHeight + headerHeight

    // Skia uses ARGB packed int
    fun Color.toSkiaArgb(): Int =
        ((alpha * ARGB_MAX_CHANNEL).toInt() shl ARGB_ALPHA_SHIFT) or
            ((red * ARGB_MAX_CHANNEL).toInt() shl ARGB_RED_SHIFT) or
            ((green * ARGB_MAX_CHANNEL).toInt() shl ARGB_GREEN_SHIFT) or
            ((blue * ARGB_MAX_CHANNEL).toInt() shl ARGB_BLUE_SHIFT)

    val surface = Surface.makeRasterN32Premul(width, totalHeight)
    val canvas: Canvas = surface.canvas

    // — Header background —
    val bgPaint = Paint().apply { color = backgroundColor.toSkiaArgb() }
    canvas.drawRect(
        Rect.makeXYWH(0f, 0f, width.toFloat(), headerHeight.toFloat()),
        bgPaint,
    )

    val textPaint = Paint().apply { color = textColor.toSkiaArgb() }

    // — Title: "KRAIL" — bold, centred
    // Empty string "" fails on iOS — CoreText bridge can't resolve a nameless family and returns
    // null, falling back to makeEmpty() with no weight. "Helvetica Neue" is guaranteed present
    // on every iOS version and resolves correctly with bold/regular weights via CoreText.
    val boldTypeface: Typeface =
        FontMgr.default.matchFamilyStyle("Helvetica Neue", FontStyle.BOLD)
            ?: Typeface.makeEmpty()
    val titleFont = Font(boldTypeface, titleSizePx)
    val titleLine = TextLine.make(titleText, titleFont)
    val titleX = (width - titleLine.width) / 2f
    canvas.drawTextLine(titleLine, titleX, titleBaseline, textPaint)

    // — Subtitle: URL — bold, centred
    val boldSubtitleTypeface: Typeface =
        FontMgr.default.matchFamilyStyle("Helvetica Neue", FontStyle.BOLD)
            ?: Typeface.makeEmpty()
    val subtitleFont = Font(boldSubtitleTypeface, subtitleSizePx)
    val subtitleLine = TextLine.make(subtitleText, subtitleFont)
    val subtitleX = (width - subtitleLine.width) / 2f
    canvas.drawTextLine(subtitleLine, subtitleX, subtitleBaseline, textPaint)

    // — Original card image below the header —
    val originalImage = Image.makeFromBitmap(originalBitmap)
    canvas.drawImage(originalImage, 0f, headerHeight.toFloat())

    return surface.makeImageSnapshot().toComposeImageBitmap()
}
