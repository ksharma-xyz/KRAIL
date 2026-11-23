package xyz.ksharma.krail.trip.planner.ui.intro

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * The drawn pixels of a node of the screen under test, for a test that holds what the rider sees and not what a function returns.
 * The node is captured by Roborazzi to a temporary picture that is read back, because the compose test's own `captureToImage` waits
 * for a frame that a Robolectric run never delivers. Roborazzi only writes when its record switch is on, so the switch is on for the
 * one capture and put back as it was, and no other test in the run is changed by it.
 */
internal class IntroPixels(private val composeRule: ComposeContentTestRule) {

    /** The node tagged [tag] as it is drawn, one pixel to a pixel of the screen. */
    fun of(tag: String): BufferedImage {
        val key = "roborazzi.test.record"
        val before = System.getProperty(key)
        val file = File.createTempFile("intro-pixels", ".png")
        try {
            System.setProperty(key, "true")
            composeRule.onNodeWithTag(tag).captureRoboImage(file.path)
            return ImageIO.read(file)
        } finally {
            if (before == null) System.clearProperty(key) else System.setProperty(key, before)
            file.delete()
        }
    }
}

/** The colour at [x], [y] of the picture, clamped to it, with the alpha it was drawn with. */
internal fun BufferedImage.colourAt(x: Float, y: Float): Color =
    Color(getRGB(x.toInt().coerceIn(0, width - 1), y.toInt().coerceIn(0, height - 1)))
