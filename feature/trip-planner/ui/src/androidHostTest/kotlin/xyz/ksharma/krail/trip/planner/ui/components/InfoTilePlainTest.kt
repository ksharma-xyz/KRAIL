package xyz.ksharma.krail.trip.planner.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.info.tile.state.InfoTileCta
import xyz.ksharma.krail.info.tile.state.InfoTileData
import xyz.ksharma.krail.info.tile.state.InfoTileState
import xyz.ksharma.krail.info.tiles.ui.InfoTile
import xyz.ksharma.krail.info.tiles.ui.InfoTileStyle
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.PreviewTheme
import xyz.ksharma.krail.taj.themeSolidBackgroundColor
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * The info tile's `Plain` style and its actions row, read from the real tile: the tint as the fill and no drop shadow,
 * and a call to action that wraps whole to its own line at large text instead of breaking in the middle of a word.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-notnight", manifest = Config.NONE)
class InfoTilePlainTest {

    @get:Rule
    val composeRule = createComposeRule()

    private var page = Color.Unspecified
    private var tint = Color.Unspecified
    private var composeView: View? = null

    private var styleState by mutableStateOf(InfoTileStyle.Plain)
    private var ctaState by mutableStateOf("Settings")
    private var scaleState by mutableStateOf(1f)
    private var openState by mutableStateOf(InfoTileState.EXPANDED)
    private var contentSet = false

    /** Shows the tile as asked. The rule allows one `setContent` a test, so a second call moves the same content's state. */
    private fun setTile(
        style: InfoTileStyle,
        cta: String = "Settings",
        fontScale: Float = 1f,
        state: InfoTileState = InfoTileState.EXPANDED,
    ) {
        if (!contentSet) {
            contentSet = true
            composeRule.setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, scaleState)) {
                    PreviewTheme {
                        composeView = LocalView.current
                        page = KrailTheme.colors.surface
                        tint = themeSolidBackgroundColor()
                        Box(modifier = Modifier.fillMaxSize().background(page).padding(EDGE)) {
                            // A new key a new tile, so the tile's own expanded state starts from the one asked for.
                            key(openState) {
                                InfoTile(
                                    infoTileData = InfoTileData(
                                        key = "location_off",
                                        title = "Location is off",
                                        description = "Turn on location to see stops near you",
                                        dismissCtaText = "Not now",
                                        primaryCta = InfoTileCta(text = ctaState),
                                    ),
                                    initialState = openState,
                                    style = styleState,
                                    onCtaClick = {},
                                    onDismissClick = {},
                                    modifier = Modifier.testTag(TILE),
                                )
                            }
                        }
                    }
                }
            }
        }
        composeRule.runOnIdle {
            styleState = style
            ctaState = cta
            scaleState = fontScale
            openState = state
        }
        composeRule.waitForIdle()
    }

    private fun drawnPixels(): Bitmap {
        var bitmap: Bitmap? = null
        composeRule.runOnUiThread {
            val view = checkNotNull(composeView)
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(checkNotNull(bitmap)))
        }
        return checkNotNull(bitmap)
    }

    private fun Bitmap.at(x: Dp, y: Dp): Color {
        val density = composeRule.density
        return Color(getPixel(with(density) { x.roundToPx() }, with(density) { y.roundToPx() }))
    }

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun textBounds(text: String): DpRect = composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun lineCountOf(text: String): Int {
        val results = mutableListOf<TextLayoutResult>()
        val node: SemanticsNode = composeRule.onNodeWithText(text, useUnmergedTree = true).fetchSemanticsNode()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertTrue(results.isNotEmpty(), "$text is text that can be measured.")
        return results.first().lineCount
    }

    // region fill and shadow

    @Test
    fun `Plain has no drop shadow, the page just outside its edge is the page's own colour`() {
        setTile(InfoTileStyle.Plain)

        val tile = boundsOf(TILE)
        val image = drawnPixels()
        val midY = tile.top + tile.height / 2f
        // 3 dp outside the start edge is where the Standard glow is strongest.
        assertClose(page, image.at(tile.left - 3.dp, midY), "just outside a Plain tile")
    }

    @Test
    fun `Standard still wears its glow, so the Plain check can tell the two apart`() {
        setTile(InfoTileStyle.Standard)

        val tile = boundsOf(TILE)
        val image = drawnPixels()
        val midY = tile.top + tile.height / 2f
        assertNotEquals(page, image.at(tile.left - 3.dp, midY).copy(alpha = 1f), "The glow tints the page outside the tile.")
    }

    @Test
    fun `Plain is filled with the solid theme tint and not the surface`() {
        setTile(InfoTileStyle.Plain)

        val tile = boundsOf(TILE)
        val image = drawnPixels()
        // A point clear of the text and the border: 8 dp in from the end edge, in the top padding.
        val inTile = image.at(tile.right - 8.dp, tile.top + 8.dp)
        assertClose(tint, inTile, "the tile's fill")
        assertNotEquals(page, tint, "The tint is not the page's own surface.")
    }

    // endregion

    // region actions

    @Test
    fun `the dismiss and the call to action share one line at 411 dp and 100 percent text`() {
        setTile(InfoTileStyle.Plain)

        val dismiss = textBounds("Not now")
        val cta = textBounds("Settings")
        assertTrue(dismiss.right <= cta.left, "Not now is before the call to action.")
        assertTrue(abs((centreY(dismiss) - centreY(cta)).value) <= 2f, "They sit on one centre line.")
        assertEquals(1, lineCountOf("Settings"))
        assertEquals(1, lineCountOf("Not now"))
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the call to action never breaks in a word at 320 dp and 200 percent, it wraps to its own line`() {
        for (cta in listOf("Settings", "Allow")) {
            setTile(InfoTileStyle.Plain, cta = cta, fontScale = 2f)

            assertEquals(1, lineCountOf(cta), "$cta is one line, never Setti and ngs.")
            assertEquals(1, lineCountOf("Not now"), "Not now is one line.")
            val dismiss = textBounds("Not now")
            val button = textBounds(cta)
            assertTrue(button.top >= dismiss.bottom - 1.dp, "$cta drops under Not now as a whole item.")
        }
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `Standard and Plain share the same actions at 200 percent, the fix is not a Plain only change`() {
        setTile(InfoTileStyle.Standard, fontScale = 2f)

        assertEquals(1, lineCountOf("Settings"))
        assertTrue(textBounds("Settings").top >= textBounds("Not now").bottom - 1.dp)
    }

    // endregion

    // region heights

    @Test
    fun `an expanded Plain tile and a Standard one are the same height, the style changes the paint only`() {
        setTile(InfoTileStyle.Plain)
        val plain = boundsOf(TILE).height

        setTile(InfoTileStyle.Standard)
        val standard = boundsOf(TILE).height

        assertNear("Plain against Standard", standard, plain)
    }

    @Test
    fun `a collapsed Plain tile is the title row alone, shorter than the expanded one`() {
        setTile(InfoTileStyle.Plain, state = InfoTileState.COLLAPSED)
        val collapsed = boundsOf(TILE).height
        setTile(InfoTileStyle.Plain)
        val expanded = boundsOf(TILE).height

        assertTrue(collapsed < expanded, "Collapsed $collapsed is under expanded $expanded.")
        assertTrue(collapsed <= 56.dp, "Collapsed is the title row and its padding, $collapsed.")
    }

    @Test
    fun `the expanded tile is one line of description at 100 percent and wraps taller at 200 percent`() {
        setTile(InfoTileStyle.Plain)
        val rest = boundsOf(TILE).height
        assertTrue(rest in TILE_HEIGHT_100_MIN..TILE_HEIGHT_100_MAX, "Expanded at 100 percent is about 132 dp, was $rest.")
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-notnight")
    fun `the expanded tile at 320 dp and 200 percent is taller than at 100 percent, with the call to action on its own line`() {
        setTile(InfoTileStyle.Plain)
        val rest = boundsOf(TILE).height
        setTile(InfoTileStyle.Plain, fontScale = 2f)
        val large = boundsOf(TILE).height

        assertTrue(large > rest + 100.dp, "The wrapped tile grows: $rest to $large.")
    }

    // endregion

    private fun centreY(rect: DpRect): Dp = rect.top + rect.height / 2f

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(abs((expected - actual).value) <= 1f, "$what: expected $expected, got $actual")
    }

    private fun assertClose(expected: Color, actual: Color, what: String) {
        val within = 0.03f
        assertTrue(
            abs(expected.red - actual.red) <= within && abs(expected.green - actual.green) <= within &&
                abs(expected.blue - actual.blue) <= within,
            "$what: expected $expected, got $actual",
        )
    }

    private companion object {
        const val TILE = "info_tile_under_test"
        val EDGE = 24.dp
        val TILE_HEIGHT_100_MIN = 120.dp
        val TILE_HEIGHT_100_MAX = 145.dp
    }
}
