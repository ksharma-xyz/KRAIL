package xyz.ksharma.krail.trip.planner.ui.savedtrips

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import xyz.ksharma.krail.taj.LocalThemeColor
import xyz.ksharma.krail.taj.hexToComposeColor
import xyz.ksharma.krail.taj.theme.KrailTheme
import xyz.ksharma.krail.taj.theme.KrailThemeStyle
import xyz.ksharma.krail.taj.theme.ThemeController
import xyz.ksharma.krail.taj.theme.ThemeMode
import xyz.ksharma.krail.trip.planner.ui.TripPlannerTestTags
import xyz.ksharma.krail.trip.planner.ui.getVisibleBoundsInRoot
import xyz.ksharma.krail.trip.planner.ui.intro.IntroPixels
import xyz.ksharma.krail.trip.planner.ui.intro.colourAt
import xyz.ksharma.krail.trip.planner.ui.state.savedtrip.StopLabel
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The label row on its own, in a window of its own size (density 1, so a dp is a pixel): every label shown, set and unset,
 * left aligned from the 16 dp edge with 8 dp between pills, equal cells while they all fit, each pill as wide as its name past that,
 * a hard cut at the trailing edge with no fade, one line per pill at any text size, the two looks, the screen reader sentences,
 * and the taps. What the row does inside the list (above the heading, hidden while the cards are reordered, nothing for no labels)
 * is in `SavedTripsHomeLayoutTest`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = PHONE)
class HomeLabelRowLayoutTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    // region which labels, and where

    @Test
    fun `every label is shown, set and unset, Home then Work then the rider's own`() {
        setRow(MIXED)

        val names = listOf("Home", "Work", "Uni", "Gym")
        names.forEach {
            composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip(it), useUnmergedTree = true).assertIsDisplayed()
        }
        val lefts = names.map { bounds(TripPlannerTestTags.homeLabelChip(it)).left }
        assertEquals(lefts.sorted(), lefts, "Home, Work, Uni, Gym run from the left in that order.")
        textIn("Home", "Home")
        textIn("Work", "+ Work")
        textIn("Uni", "Uni")
        textIn("Gym", "+ Gym")
    }

    @Test
    fun `the first pill starts at the 16 dp leading edge and the pills are 8 dp apart`() {
        setRow(MIXED)

        val first = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        assertNear("the first pill's left edge", 16.dp, first.left)
        listOf("Home", "Work", "Uni", "Gym").zipWithNext().forEach { (a, b) ->
            assertNear(
                "the gap from $a to $b",
                8.dp,
                bounds(TripPlannerTestTags.homeLabelChip(b)).left - bounds(TripPlannerTestTags.homeLabelChip(a)).right,
            )
        }
    }

    @Test
    fun `the row is left aligned, so two labels do not centre or stretch`() {
        setRow(persistentListOf(StopLabel.defaults[0], StopLabel.defaults[1]))

        assertNear("Home starts at the edge", 16.dp, bounds(TripPlannerTestTags.homeLabelChip("Home")).left)
        assertTrue(
            bounds(TripPlannerTestTags.homeLabelChip("Work")).right < PHONE_WIDTH / 2,
            "Two pills stay at the start of the row and do not fill it.",
        )
    }

    // endregion

    // region cells

    @Test
    fun `while every pill fits the cells are equal in width`() {
        setRow(MIXED)

        val widths = listOf("Home", "Work", "Uni", "Gym").map { bounds(TripPlannerTestTags.homeLabelChip(it)).width }
        assertTrue(
            widths.all { abs(it.value - widths.first().value) <= TOLERANCE_DP },
            "The four cells are one width: $widths",
        )
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `once they do not fit each pill is as wide as its own name`() {
        setRow(MANY)

        // A pill scrolled wholly into view shows its whole width, which is the width it was laid out at.
        val widths = MANY_NAMES.map { name ->
            composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip(name), useUnmergedTree = true).performScrollTo()
            composeRule.waitForIdle()
            bounds(TripPlannerTestTags.homeLabelChip(name)).width
        }
        assertTrue(widths.distinct().size > 1, "The pills are not equal once the row overflows: $widths")
        val short = widths[MANY_NAMES.indexOf("Uni")]
        val long = widths[MANY_NAMES.indexOf(LONG)]
        assertTrue(long > short + 60.dp, "A long name makes its pill wider: $short and $long, all widths $widths")
    }

    // endregion

    // region overflow

    @Test
    @Config(qualifiers = NARROW)
    fun `an overflowing row is cut hard by the trailing edge, with no fade`() {
        setRow(STRADDLE, page = Color.White)

        val pill = bounds(TripPlannerTestTags.homeLabelChip(LONG))
        assertNear("the long pill is cut by the trailing edge of the row", NARROW_WIDTH, pill.right)
        assertTrue(pill.width > 40.dp && pill.width < 150.dp, "Part of the long pill shows, and not all of it: $pill")

        // The pixels at the very edge are the pill's own fill, the same as the ones well inside it: a mask or a gradient would
        // blend them towards the page. Read on the pill's top row, above its text.
        val picture = IntroPixels(composeRule).of(ROW_HOST)
        val y = pill.top.value + 3f
        val inside = picture.colourAt(pill.right.value - 24f, y)
        val atEdge = picture.colourAt(pill.right.value - 1f, y)
        assertEquals(THEME_COLOUR.toArgb(), inside.toArgb(), "The pill is the theme colour inside the row.")
        assertEquals(
            inside.toArgb(),
            atEdge.toArgb(),
            "The last pixels of the cut pill are as solid as the ones 24 px inside: no fade.",
        )
    }

    @Test
    @Config(qualifiers = NARROW)
    fun `scrolled to the end the last pill is whole and the content ends 16 dp short of the edge`() {
        setRow(MANY)

        composeRule.onNode(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange),
            useUnmergedTree = true,
        )
            .performSemanticsAction(SemanticsActions.ScrollBy) { it(100_000f, 0f) }
        composeRule.waitForIdle()

        val last = bounds(TripPlannerTestTags.homeLabelChip(MANY_NAMES.last()))
        assertNear("the last pill ends 16 dp before the trailing edge", NARROW_WIDTH - 16.dp, last.right)
    }

    // endregion

    // region pills

    @Test
    @Config(qualifiers = NARROW)
    fun `at 200 percent a pill is one line and is never truncated, a long name makes it wider`() {
        setRow(MANY, fontScale = 2f)

        MANY_NAMES.forEach { name ->
            val text = if (name in SET_NAMES) name else "+ $name"
            val layout = textLayout(name, text)
            assertEquals(1, layout.lineCount, "$name is one line at 200 percent")
            assertTrue(!layout.isLineEllipsized(0), "$name is not cut")
        }
    }

    @Test
    fun `every pill is the same height, set or unset, in a touch slot of at least 48 dp`() {
        setRow(MIXED)

        val names = listOf("Home", "Work", "Uni", "Gym")
        val drawn = names.map { bounds(TripPlannerTestTags.homeLabelChip(it)).height }
        val slots = names.map {
            composeRule.onNodeWithTag(
                TripPlannerTestTags.homeLabelChip(it),
                useUnmergedTree = true,
            ).fetchSemanticsNode().touchBoundsInRoot.height
        }
        assertTrue(
            drawn.all { abs(it.value - drawn.first().value) <= 1f },
            "The four pills are drawn one height: $drawn",
        )
        assertTrue(slots.all { it >= 48f }, "Each touch slot is at least 48 dp: $slots")
    }

    @Test
    fun `a set label is filled in the theme colour and an unset one is outlined with no fill, both text only`() {
        setRow(MIXED, page = Color.White)

        val picture = IntroPixels(composeRule).of(ROW_HOST)
        val home = bounds(TripPlannerTestTags.homeLabelChip("Home"))
        val work = bounds(TripPlannerTestTags.homeLabelChip("Work"))
        // Four px into the pill, level with its middle: in its rounded end, before the text, so only the fill or the page is there.
        assertEquals(
            THEME_COLOUR.toArgb(),
            picture.colourAt(home.left.value + 4f, home.top.value + home.height.value / 2).toArgb(),
            "A set label's fill is the theme colour.",
        )
        assertEquals(
            Color.White.toArgb(),
            picture.colourAt(work.left.value + 8f, work.top.value + work.height.value / 2).toArgb(),
            "An unset label has no fill, so the page shows.",
        )
        // Text only, no icon: each pill holds exactly one text node, and the label's own words are in it.
        listOf("Home", "Work").forEach { name ->
            val inPill = composeRule.onAllNodes(
                hasAnyAncestor(hasTestTag(TripPlannerTestTags.homeLabelChip(name))),
                useUnmergedTree = true,
            ).fetchSemanticsNodes()
            val texts = inPill.count { SemanticsProperties.Text in it.config }
            assertEquals(1, texts, "$name holds one text and no icon: ${inPill.size} nodes")
        }
    }

    // endregion

    // region screen reader and taps

    @Test
    fun `a screen reader hears each pill's sentence, as a button`() {
        setRow(MIXED)

        assertEquals(
            listOf("Home, double tap to go there"),
            composeRule.onNodeWithTag(
                TripPlannerTestTags.homeLabelChip("Home"),
            ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription),
        )
        assertEquals(
            listOf("Work, not set, double tap to choose a stop"),
            composeRule.onNodeWithTag(
                TripPlannerTestTags.homeLabelChip("Work"),
            ).fetchSemanticsNode().config.getOrNull(SemanticsProperties.ContentDescription),
        )
        assertEquals(
            "Gym, not set, double tap to choose a stop",
            homeLabelDescription(StopLabel(emoji = "x", label = "Gym")),
        )
    }

    @Test
    fun `a tap on any pill raises its label's key`() {
        setRow(MIXED)

        listOf("Home", "Work", "Uni", "Gym").forEach { name ->
            composeRule.onNodeWithTag(TripPlannerTestTags.homeLabelChip(name), useUnmergedTree = true).performClick()
        }

        assertEquals(listOf("Home", "Work", "Uni", "Gym"), clicks)
    }

    // endregion

    // region which labels the row is given

    @Test
    fun `the labels are Home then Work then the rider's own, a deleted Work is still there as unset, and none until loaded`() {
        assertEquals(emptyList(), homeLabelsFor(emptyList()))
        val onlyHome = homeLabelsFor(listOf(StopLabel(emoji = "H", label = "Home", stopId = "1", stopName = "Central")))
        assertEquals(listOf("Home", "Work"), onlyHome.map { it.label })
        assertTrue(onlyHome[0].isSet && !onlyHome[1].isSet)
        val custom = homeLabelsFor(
            listOf(
                StopLabel(emoji = "G", label = "Gym"),
                StopLabel(emoji = "W", label = "Work", stopId = "2", stopName = "Town Hall"),
                StopLabel(emoji = "H", label = "home"),
            ),
        )
        val names = custom.map { it.label }
        assertEquals(
            listOf("home", "Work", "Gym"),
            names,
            "Built in labels first, the rider's own after, in their order.",
        )
    }

    // endregion

    // region harness

    private fun setRow(
        labels: List<StopLabel>,
        fontScale: Float = 1f,
        page: Color = Color.Unspecified,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                KrailTheme(themeController = ThemeController(currentMode = ThemeMode.LIGHT, setThemeMode = {})) {
                    CompositionLocalProvider(
                        LocalThemeColor provides remember { mutableStateOf(KrailThemeStyle.Train.hexColorCode) },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(if (page != Color.Unspecified) Modifier.background(page) else Modifier)
                                .testTag(ROW_HOST),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            HomeLabelRow(labels = labels.toImmutableList(), onLabelClick = { clicks += it })
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()
    }

    private fun textLayout(label: String, text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        val node = composeRule.onNode(
            hasText(text) and hasAnyAncestor(hasTestTag(TripPlannerTestTags.homeLabelChip(label))),
            useUnmergedTree = true,
        ).fetchSemanticsNode()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        return layouts.first()
    }

    private fun textIn(label: String, text: String) {
        composeRule.onNode(
            hasText(text) and hasAnyAncestor(hasTestTag(TripPlannerTestTags.homeLabelChip(label))),
            useUnmergedTree = true,
        ).assertExists()
    }

    private fun bounds(tag: String): DpRect = composeRule.onNodeWithTag(
        tag,
        useUnmergedTree = true,
    ).getVisibleBoundsInRoot()

    private fun assertNear(what: String, expected: Dp, actual: Dp) {
        assertTrue(
            abs(expected.value - actual.value) <= TOLERANCE_DP,
            "$what should be at $expected but is at $actual.",
        )
    }

    // endregion

    private companion object {
        const val ROW_HOST = "test.rowhost"
        const val TOLERANCE_DP = 1.5f
        const val LONG = "Grandparents house far away"
        val THEME_COLOUR = KrailThemeStyle.Train.hexColorCode.hexToComposeColor()
        val PHONE_WIDTH = 411.dp
        val NARROW_WIDTH = 320.dp

        val MIXED = listOf(
            StopLabel(emoji = "H", label = "Home", stopId = "1", stopName = "Central Station"),
            StopLabel(emoji = "W", label = "Work"),
            StopLabel(emoji = "U", label = "Uni", stopId = "2", stopName = "Redfern Station"),
            StopLabel(emoji = "G", label = "Gym"),
        )

        // Home, Work and Uni fit with 90 dp to spare, so the long pill, which is set and so filled, is cut by the 320 dp edge.
        val STRADDLE = listOf(
            StopLabel(emoji = "H", label = "Home", stopId = "1", stopName = "Central Station"),
            StopLabel(emoji = "W", label = "Work"),
            StopLabel(emoji = "U", label = "Uni", stopId = "2", stopName = "Redfern Station"),
            StopLabel(emoji = "x", label = LONG, stopId = "3", stopName = "Marrickville Station"),
        )

        val SET_NAMES = setOf("Home", "Uni", "Beach", "Cafe")
        val MANY_NAMES = listOf("Home", "Work", "Uni", "Gym", LONG, "Beach", "Library", "Cafe")
        val MANY = MANY_NAMES.map { name ->
            if (name in SET_NAMES) {
                StopLabel(emoji = "x", label = name, stopId = "id$name", stopName = "$name Station")
            } else {
                StopLabel(emoji = "x", label = name)
            }
        }
    }
}

private const val PHONE = "w411dp-h891dp"
private const val NARROW = "w320dp-h1280dp"
