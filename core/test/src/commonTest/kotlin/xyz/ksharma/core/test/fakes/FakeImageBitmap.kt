package xyz.ksharma.core.test.fakes

import androidx.compose.ui.graphics.colorspace.ColorSpace
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig

class FakeImageBitmap : ImageBitmap {
    override val width = 1
    override val height = 1
    override val config = ImageBitmapConfig.Argb8888
    override val hasAlpha = true
    override val colorSpace: ColorSpace = ColorSpaces.Srgb

    override fun readPixels(
        buffer: IntArray,
        startX: Int,
        startY: Int,
        width: Int,
        height: Int,
        bufferOffset: Int,
        stride: Int,
    ) = Unit

    override fun prepareToDraw() = Unit
}
