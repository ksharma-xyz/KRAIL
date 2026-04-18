package xyz.ksharma.krail.core.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap

actual fun ImageBitmap.withBrandingHeader(
    titleText: String,
    subtitleText: String,
    backgroundColor: Color,
    textColor: Color,
    density: Float,
): ImageBitmap {
    // graphicsLayer.toImageBitmap() on Android returns a hardware-backed bitmap (GPU memory).
    // Software Canvas cannot draw hardware bitmaps directly — copy to ARGB_8888 in RAM first.
    val original = asAndroidBitmap().copy(
        Bitmap.Config.ARGB_8888,
        false,
    )
    val width = original.width

    // Convert sp/dp to pixels using screen density
    val titleSizePx = BRANDING_TITLE_SIZE_SP * density
    val subtitleSizePx = BRANDING_SUBTITLE_SIZE_SP * density
    val paddingPx = BRANDING_HEADER_PADDING_DP * density
    val gapPx = BRANDING_TITLE_SUBTITLE_GAP_DP * density

    // Baseline positions — drawText y is the text baseline.
    // We treat titleSizePx as the approximate ascent (works well for most system fonts).
    val titleBaseline = paddingPx + titleSizePx
    val subtitleBaseline = titleBaseline + gapPx + subtitleSizePx
    val headerHeight = (subtitleBaseline + paddingPx).toInt()

    val combined = createBitmap(width, original.height + headerHeight)
    val canvas = Canvas(combined)

    // — Header background —
    val bgPaint = Paint().apply { color = backgroundColor.toArgb() }
    canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), bgPaint)

    // — Title: "KRAIL" — bold, centred
    // Roboto is the Android system font, always present. Using it explicitly avoids
    // relying on Typeface.DEFAULT which may vary by device/OEM.
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = titleSizePx
        typeface = Typeface.create("Roboto", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(titleText, width / 2f, titleBaseline, titlePaint)

    // — Subtitle: URL — semi-bold, centred
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = subtitleSizePx
        typeface = Typeface.create("Roboto", Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(subtitleText, width / 2f, subtitleBaseline, subtitlePaint)

    // — Original card image below the header —
    canvas.drawBitmap(original, 0f, headerHeight.toFloat(), null)

    return combined.asImageBitmap()
}
