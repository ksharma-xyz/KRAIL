package xyz.ksharma.krail.trip.planner.ui.intro

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.theme.PreviewTheme
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The room of the Park & Ride card grows when the skeleton, which holds one car park, becomes the card of a station with several,
 * and the Intro's middle zone, which is centred around it, moves with it. It used to jump, by about a car park and a half for the
 * one station with three. The room now grows over [PARK_RIDE_CARD_GROWTH_MS], read here from the layout on a clock that is stepped
 * by hand, and with reduced motion it takes the station's height in the frame the station is found. A station with one car park, which is nearly all of them, does not
 * change height at all, and the skeleton still appears at its own height at once.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    sdk = [34],
    qualifiers = RobolectricDeviceQualifiers.Pixel6,
    manifest = Config.NONE,
)
class IntroParkRideCardGrowthTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val shown = mutableStateOf<IntroCardSlot>(IntroCardSlot.Skeleton)

    private val threeCarParks = introCardSlot(PreviewTallCardContentState)
    private val oneCarPark = introCardSlot(PreviewCardLoadingState)

    private fun start(initial: IntroCardSlot, reduceMotion: Boolean = false) {
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        Settings.Global.putFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, if (reduceMotion) 0f else 1f)
        shown.value = initial
        // The clock is stepped by hand, so the height can be read in every frame of the growth.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { PreviewTheme { IntroParkRideCardSlot(slot = shown.value) } }
        composeRule.mainClock.advanceTimeBy(FRAME_MS * 2)
    }

    @After
    fun restoreMotion() {
        Settings.Global.putFloat(
            ApplicationProvider.getApplicationContext<Context>().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        )
    }

    private fun height(): Float =
        composeRule.onNodeWithTag(INTRO_CARD_SLOT_TAG).getUnclippedBoundsInRoot().height.value

    // The height in each of the next [frames] frames, the first one being the frame the change is laid out in.
    private fun heightsOver(frames: Int): List<Float> = List(frames) {
        composeRule.mainClock.advanceTimeByFrame()
        height()
    }

    // The height the slot settles at once nothing is moving, which is the end of any growth.
    private fun settledHeight(): Float {
        composeRule.mainClock.advanceTimeBy(PARK_RIDE_CARD_GROWTH_MS + FRAME_MS * 4)
        return height()
    }

    // The most any easing curve of the spec can move in a window of one frame and a frame of lag, as a share of the whole
    // distance, because the clock starts a frame after the change. The most a frame may move the height is this share of it.
    private fun largestShareInOneFrame(): Float {
        val window = (FRAME_MS * 2).toFloat() / PARK_RIDE_CARD_GROWTH_MS
        return (0..STEPS).maxOf { i ->
            val at = i.toFloat() / STEPS
            FastOutSlowInEasing.transform((at + window).coerceAtMost(1f)) - FastOutSlowInEasing.transform(at)
        }
    }

    @Test
    fun `the growth is 300 ms, the length of the Intro's other changes`() {
        assertEquals(300, PARK_RIDE_CARD_GROWTH_MS)
        assertEquals(BAR_FILL_CROSSFADE_MS, PARK_RIDE_CARD_GROWTH_MS)
    }

    @Test
    fun `only a slot with a card in it animates its size, and never with reduced motion`() {
        assertEquals(false, IntroCardSlot.Empty.animatesGrowth(reduceMotion = false))
        assertEquals(true, IntroCardSlot.Skeleton.animatesGrowth(reduceMotion = false))
        assertEquals(true, threeCarParks.animatesGrowth(reduceMotion = false))
        assertEquals(true, IntroCardSlot.NoStation.animatesGrowth(reduceMotion = false))
        listOf(IntroCardSlot.Empty, IntroCardSlot.Skeleton, threeCarParks, IntroCardSlot.NoStation).forEach { slot ->
            assertEquals(false, slot.animatesGrowth(reduceMotion = true), "$slot animates with reduced motion")
        }
    }

    @Test
    fun `a station with three car parks is taller than the skeleton, so there is a growth to smooth`() {
        start(IntroCardSlot.Skeleton)
        val skeleton = height()

        shown.value = threeCarParks
        val tall = settledHeight()

        assertTrue(tall - skeleton >= TWO_CAR_PARKS_DP, "the skeleton is $skeleton dp and three car parks are $tall dp")
    }

    @Test
    fun `from the skeleton to three car parks the height grows over 300 ms with no frame that jumps by more than the curve allows`() {
        start(IntroCardSlot.Skeleton)
        val skeleton = height()
        shown.value = threeCarParks
        val frames = heightsOver(PARK_RIDE_CARD_GROWTH_MS / FRAME_MS.toInt() + 4)
        val tall = settledHeight()
        val distance = tall - skeleton
        val allowedStep = distance * largestShareInOneFrame() + ROUNDING_DP

        assertTrue(abs(frames.first() - skeleton) <= allowedStep, "the first frame is ${frames.first()} dp, not near the skeleton's $skeleton")
        frames.zipWithNext().forEachIndexed { index, (before, after) ->
            assertTrue(after >= before - ROUNDING_DP, "the height shrinks from $before to $after in frame ${index + 2}")
            assertTrue(after - before <= allowedStep, "the height jumps $before to $after in frame ${index + 2}, over $allowedStep dp")
        }
        val between = frames.count { it > skeleton + ROUNDING_DP && it < tall - ROUNDING_DP }
        assertTrue(between >= MIN_FRAMES_BETWEEN, "only $between frames are between the two heights: $frames")
        assertTrue(abs(frames.last() - tall) <= ROUNDING_DP, "the growth has not ended after 300 ms: ${frames.last()} dp, not $tall")
    }

    @Test
    fun `it is still growing at 100 ms and has finished by the end of the spec and a frame`() {
        start(IntroCardSlot.Skeleton)
        val skeleton = height()
        shown.value = threeCarParks
        composeRule.mainClock.advanceTimeBy(FRAME_MS)
        val early = heightsOver(PARK_RIDE_CARD_GROWTH_MS / 3 / FRAME_MS.toInt()).last()
        composeRule.mainClock.advanceTimeBy(PARK_RIDE_CARD_GROWTH_MS + FRAME_MS * 2)
        val tall = height()

        assertTrue(early > skeleton + ROUNDING_DP && early < tall - TWO_CAR_PARKS_DP / 4, "at 100 ms the height is $early dp")
        assertTrue(tall - skeleton >= TWO_CAR_PARKS_DP, "the growth ended at $tall dp from $skeleton dp")
    }

    @Test
    fun `a station with one car park does not change height in any frame`() {
        start(IntroCardSlot.Skeleton)
        val skeleton = height()
        shown.value = oneCarPark
        val frames = heightsOver(PARK_RIDE_CARD_GROWTH_MS / FRAME_MS.toInt() + 4)

        assertTrue(frames.all { abs(it - skeleton) <= ROUNDING_DP }, "the height moved from $skeleton dp: $frames")
        assertTrue(abs(settledHeight() - skeleton) <= ROUNDING_DP, "the settled height is not the skeleton's $skeleton dp")
    }

    @Test
    fun `with reduced motion the height is the station's in the frame the station is found and is never between`() {
        start(IntroCardSlot.Skeleton, reduceMotion = true)
        val skeleton = height()
        shown.value = threeCarParks
        val frames = heightsOver(FRAMES_WITH_NO_ANIMATION)
        val tall = settledHeight()

        assertTrue(tall - skeleton >= TWO_CAR_PARKS_DP, "the skeleton is $skeleton dp and three car parks are $tall dp")
        assertTrue(abs(frames.first() - tall) <= ROUNDING_DP, "the first frame is ${frames.first()} dp, not the station's $tall: $frames")
        assertTrue(frames.all { abs(it - tall) <= ROUNDING_DP }, "a frame is between the two heights: $frames")
    }

    @Test
    fun `the skeleton still appears at its own height in the first frame, because the empty slot has nothing to grow from`() {
        start(IntroCardSlot.Empty)
        assertEquals(0f, height())

        shown.value = IntroCardSlot.Skeleton
        val first = heightsOver(1).single()
        val skeleton = settledHeight()

        assertTrue(skeleton > 0f, "the skeleton has no height")
        assertTrue(abs(first - skeleton) <= ROUNDING_DP, "the skeleton grew from nothing, $first dp in its first frame, not $skeleton")
    }

    @Test
    fun `a card that leaves goes at once, as it did`() {
        start(introCardSlot(PreviewCardContentState))
        assertTrue(height() > 0f)

        shown.value = IntroCardSlot.Empty
        val first = heightsOver(1).single()

        assertEquals(0f, first, "the card shrank over time, $first dp in the first frame")
    }

    private companion object {
        const val FRAME_MS = 16L
        const val STEPS = 200

        // A pixel at the phone's density is about 0.4 dp, and a layout rounds each edge to one.
        const val ROUNDING_DP = 1f

        // The skeleton holds one car park and three is two more, each a name and its figures, which is well over this.
        const val TWO_CAR_PARKS_DP = 100f
        const val MIN_FRAMES_BETWEEN = 8

        // The frames read after a change that has no animation, which all show the station's height.
        const val FRAMES_WITH_NO_ANIMATION = 3
    }
}
