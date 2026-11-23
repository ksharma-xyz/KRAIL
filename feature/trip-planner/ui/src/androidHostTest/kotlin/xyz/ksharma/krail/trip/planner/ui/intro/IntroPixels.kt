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
 * for a frame that a Robolectric run never delivers. Roborazzi only writes the picture when it is in record mode, and under the
 * verify and compare tasks it compares against a golden that does not exist and writes nothing. So for the one capture the record
 * switch is on and the verify and compare switches are off, and all three are put back as they were, so no other test in the run
 * is changed by it.
 */
internal class IntroPixels(private val composeRule: ComposeContentTestRule) {

    /** The node tagged [tag] as it is drawn, one pixel to a pixel of the screen. */
    fun of(tag: String): BufferedImage {
        val before = SWITCHES.associateWith { System.getProperty(it) }
        val file = File.createTempFile("intro-pixels", ".png")
        try {
            SWITCHES.forEach { System.setProperty(it, if (it == RECORD) "true" else "false") }
            composeRule.onNodeWithTag(tag).captureRoboImage(file.path)
            return ImageIO.read(file)
        } finally {
            before.forEach { (key, value) -> if (value == null) System.clearProperty(key) else System.setProperty(key, value) }
            file.delete()
        }
    }

    private companion object {
        const val RECORD = "roborazzi.test.record"
        val SWITCHES = listOf(RECORD, "roborazzi.test.verify", "roborazzi.test.compare")
    }
}

/** The colour at [x], [y] of the picture, clamped to it, with the alpha it was drawn with. */
internal fun BufferedImage.colourAt(x: Float, y: Float): Color =
    Color(getRGB(x.toInt().coerceIn(0, width - 1), y.toInt().coerceIn(0, height - 1)))
