package xyz.ksharma.krail.core.adaptiveui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pins the breakpoint contract documented in `docs/TABLET_FOLDABLE_UX.md` §1.
 *
 * Every screen that splits into panes asks this one object which world it is in, so a one-dp
 * drift here silently re-lays-out the whole app on a class of device nobody in the room owns.
 * The values that matter are the boundaries themselves, and each case below is stated as the
 * pair either side of one: 599/600, 839/840, 479/480. A test that only sampled 400 dp and
 * 1000 dp would stay green through an off-by-one.
 *
 * The derived flags are pinned as well as the raw classes, because they are what call sites
 * actually read. `shouldShowDualPane` decides whether a map exists at all; `isPhoneLandscape`
 * decides whether the home screen's search row collapses to a pill; `isTablet` gates the
 * adaptations that need both dimensions roomy.
 */
class AdaptiveLayoutInfoTest {

    @Test
    fun widthClassBoundariesAreExact() {
        val cases = listOf(
            WidthCase(0, WindowWidthSizeClass.COMPACT),
            WidthCase(LAST_COMPACT_WIDTH, WindowWidthSizeClass.COMPACT),
            WidthCase(FIRST_MEDIUM_WIDTH, WindowWidthSizeClass.MEDIUM),
            WidthCase(LAST_MEDIUM_WIDTH, WindowWidthSizeClass.MEDIUM),
            WidthCase(FIRST_EXPANDED_WIDTH, WindowWidthSizeClass.EXPANDED),
            WidthCase(LAST_EXPANDED_WIDTH, WindowWidthSizeClass.EXPANDED),
            WidthCase(FIRST_LARGE_WIDTH, WindowWidthSizeClass.LARGE),
            WidthCase(LAST_LARGE_WIDTH, WindowWidthSizeClass.LARGE),
            WidthCase(FIRST_EXTRA_LARGE_WIDTH, WindowWidthSizeClass.EXTRA_LARGE),
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                adaptiveLayoutInfoFor(widthDp = case.widthDp, heightDp = ROOMY_HEIGHT)
                    .widthSizeClass,
                "width ${case.widthDp}dp should classify as ${case.expected}",
            )
        }
    }

    @Test
    fun heightClassBoundariesAreExact() {
        val cases = listOf(
            HeightCase(0, WindowHeightSizeClass.COMPACT),
            HeightCase(LAST_COMPACT_HEIGHT, WindowHeightSizeClass.COMPACT),
            HeightCase(FIRST_MEDIUM_HEIGHT, WindowHeightSizeClass.MEDIUM),
            HeightCase(LAST_MEDIUM_HEIGHT, WindowHeightSizeClass.MEDIUM),
            HeightCase(FIRST_EXPANDED_HEIGHT, WindowHeightSizeClass.EXPANDED),
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                adaptiveLayoutInfoFor(widthDp = ROOMY_WIDTH, heightDp = case.heightDp)
                    .heightSizeClass,
                "height ${case.heightDp}dp should classify as ${case.expected}",
            )
        }
    }

    @Test
    fun dualPaneStartsAtSixHundredDpWide() {
        // The gate for "is there a second pane at all". Height never enters into it: a phone in
        // landscape is wide enough for two panes and gets them.
        assertFalse(
            adaptiveLayoutInfoFor(LAST_COMPACT_WIDTH, ROOMY_HEIGHT).shouldShowDualPane,
            "${LAST_COMPACT_WIDTH}dp is still a single pane",
        )
        assertTrue(
            adaptiveLayoutInfoFor(FIRST_MEDIUM_WIDTH, ROOMY_HEIGHT).shouldShowDualPane,
            "${FIRST_MEDIUM_WIDTH}dp is where dual pane begins",
        )
        assertTrue(
            adaptiveLayoutInfoFor(FIRST_MEDIUM_WIDTH, LAST_COMPACT_HEIGHT).shouldShowDualPane,
            "a short window that is wide enough still gets two panes",
        )
    }

    @Test
    fun phoneLandscapeIsExactlyCompactHeight() {
        // Not a convenience alias but the whole definition: width cannot tell a phone in
        // landscape from a tablet, because an edge-to-edge phone reaches EXPANDED width when
        // rotated. Height can. If these two ever diverge the pill on the home screen starts
        // appearing on tablets.
        val widths = listOf(
            LAST_COMPACT_WIDTH,
            FIRST_MEDIUM_WIDTH,
            FIRST_EXPANDED_WIDTH,
            FIRST_EXTRA_LARGE_WIDTH,
        )
        val heights = listOf(
            0,
            LAST_COMPACT_HEIGHT,
            FIRST_MEDIUM_HEIGHT,
            FIRST_EXPANDED_HEIGHT,
        )

        widths.forEach { width ->
            heights.forEach { height ->
                val info = adaptiveLayoutInfoFor(width, height)
                assertEquals(
                    info.isCompactHeight,
                    info.isPhoneLandscape,
                    "isPhoneLandscape must equal isCompactHeight at ${width}x${height}dp",
                )
            }
        }
    }

    @Test
    fun tabletNeedsBothDimensionsRoomy() {
        assertFalse(
            adaptiveLayoutInfoFor(FIRST_EXPANDED_WIDTH, LAST_COMPACT_HEIGHT).isTablet,
            "wide but short is a phone in landscape",
        )
        assertFalse(
            adaptiveLayoutInfoFor(LAST_COMPACT_WIDTH, FIRST_EXPANDED_HEIGHT).isTablet,
            "tall but narrow is a phone in portrait",
        )
        assertTrue(
            adaptiveLayoutInfoFor(FIRST_MEDIUM_WIDTH, FIRST_MEDIUM_HEIGHT).isTablet,
            "the first window that is neither compact is a tablet",
        )
    }

    @Test
    fun rawDimensionsAreCarriedThroughUntouched() {
        // Call sites read widthDp/heightDp for their own maths, so the classification must not
        // be the only thing that survives.
        val info = adaptiveLayoutInfoFor(widthDp = 731, heightDp = 517)

        assertEquals(731, info.widthDp)
        assertEquals(517, info.heightDp)
    }

    private data class WidthCase(val widthDp: Int, val expected: WindowWidthSizeClass)

    private data class HeightCase(val heightDp: Int, val expected: WindowHeightSizeClass)

    private companion object {
        // Every breakpoint constant is the FIRST value of the class above it, so each pair
        // below is written out rather than derived, and a change to the constants shows up
        // here as a failure rather than as a silently-shifted test.
        const val LAST_COMPACT_WIDTH = 599
        const val FIRST_MEDIUM_WIDTH = 600
        const val LAST_MEDIUM_WIDTH = 839
        const val FIRST_EXPANDED_WIDTH = 840
        const val LAST_EXPANDED_WIDTH = 1199
        const val FIRST_LARGE_WIDTH = 1200
        const val LAST_LARGE_WIDTH = 1599
        const val FIRST_EXTRA_LARGE_WIDTH = 1600

        const val LAST_COMPACT_HEIGHT = 479
        const val FIRST_MEDIUM_HEIGHT = 480
        const val LAST_MEDIUM_HEIGHT = 899
        const val FIRST_EXPANDED_HEIGHT = 900

        // Neither dimension under test in the other axis' cases.
        const val ROOMY_WIDTH = 1000
        const val ROOMY_HEIGHT = 1000
    }
}
