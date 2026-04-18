package xyz.ksharma.krail.core.share

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
    val original = asAndroidBitmap()
    val width = original.width

    // Convert sp/dp to pixels using screen density
    val titleSizePx = 22f * density      // titleLarge ~22sp
    val subtitleSizePx = 12f * density   // caption ~12sp
    val paddingPx = 24f * density        // 24dp top and bottom padding
    val gapPx = 8f * density             // 8dp gap between title and subtitle

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
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = titleSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(titleText, width / 2f, titleBaseline, titlePaint)

    // — Subtitle: URL — regular weight, centred
    val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = subtitleSizePx
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText(subtitleText, width / 2f, subtitleBaseline, subtitlePaint)

    // — Original card image below the header —
    canvas.drawBitmap(original, 0f, headerHeight.toFloat(), null)

    return combined.asImageBitmap()
}

