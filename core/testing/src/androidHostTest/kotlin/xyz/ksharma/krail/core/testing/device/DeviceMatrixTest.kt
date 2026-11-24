package xyz.ksharma.krail.core.testing.device

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.safeGestures
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Proves the device matrix can go red, and for the right reason. A helper that cannot fail is worth nothing, so each assertion
 * runs against a screen that breaks the rule it checks (the failing set is pinned, not just "something failed") and against the
 * fixed screen, which must pass on every profile.
 *
 * The screens here are fixtures: a control in every corner and on the middle of each side, the exact places a system bar takes.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class DeviceMatrixTest {

    @get:Rule
    val matrix = DeviceMatrixRule()

    private val everyProfile = DeviceProfiles.all.map { it.name }.toSet()

    // region assertNothingInteractiveUnderSystemBars

    @Test
    fun `a screen that ignores every inset fails on every profile`() {
        val failed = failedOn {
            setContent { EdgeControls(Modifier) }
            assertNothingInteractiveUnderSystemBars()
        }

        assertEquals(everyProfile, failed)
    }

    @Test
    fun `the failure names the profile, its numbers, where they came from and the control`() {
        val error = assertFailsWith<AssertionError> {
            matrix.forEachDeviceProfile(screen = "Fixture", profiles = listOf(DeviceProfiles.IPhoneDuoOuter)) {
                setContent { EdgeControls(Modifier) }
                assertNothingInteractiveUnderSystemBars()
            }
        }
        val message = error.message.orEmpty()

        assertTrue(message.contains("Fixture failed on 1 of 1 device profiles"), message)
        assertTrue(message.contains("[IPhoneDuoOuter] IPhoneDuoOuter 466x678 density 3.0"), message)
        assertTrue(message.contains("measured: Compose inset probe on the iPhone Duo simulator"), message)
        assertTrue(message.contains("tag 'centreEnd'"), message)
        assertTrue(message.contains("the right system region"), message)
    }

    @Test
    fun `a screen that pads by the display cutout fails exactly where iOS delivers the side inset elsewhere`() {
        // The incident: Search Stop padded by the display cutout. Android delivers a side inset as that, so it passed every
        // Android profile. iOS reports it as 0 at the sides and puts the strip in systemBars and safeDrawing.
        val failed = failedOn {
            setContent { EdgeControls(Modifier.cutoutOnlyPadding()) }
            assertNothingInteractiveUnderSystemBars()
        }

        assertEquals(
            setOf("IPhoneDuoOuter", "IPhoneDuoInnerPortrait", "IPhoneDuoInnerLandscape", "IPhoneProLandscape"),
            failed,
        )
    }

    @Test
    fun `a screen that pads by the safe drawing inset passes on every profile`() {
        matrix.forEachDeviceProfile(screen = "Fixture") {
            setContent { EdgeControls(Modifier.safeDrawingPadding()) }
            assertNothingInteractiveUnderSystemBars()
        }
    }

    @Test
    fun `a screen with no control fails instead of passing with nothing to check`() {
        val error = assertFailsWith<AssertionError> {
            matrix.forEachDeviceProfile(screen = "Fixture", profiles = listOf(DeviceProfiles.SmallPhone)) {
                setContent { Box(Modifier.fillMaxSize()) }
                assertNothingInteractiveUnderSystemBars()
            }
        }

        assertTrue(error.message.orEmpty().contains("no control on it"), error.message)
    }

    // endregion

    // region assertTouchReaches

    @Test
    fun `a touch on a control under a system region fails where the system takes it`() {
        val failed = failedOn {
            setContent { EdgeControls(Modifier) }
            assertTouchReaches(onNodeWithTag("centreEnd"))
        }

        // The control is 48 wide at the right edge: wholly under a side strip of 48 or more, so every profile with one.
        assertEquals(
            setOf(
                "AndroidLandscapeSideNavBar",
                "IPhoneProLandscape",
                "IPhoneDuoOuter",
                "IPhoneDuoInnerPortrait",
                "IPhoneDuoInnerLandscape",
            ),
            failed,
        )
    }

    @Test
    fun `a touch on a padded control reaches it on every profile`() {
        var presses = 0
        matrix.forEachDeviceProfile(screen = "Fixture") {
            presses = 0
            setContent { EdgeControls(Modifier.safeDrawingPadding(), onPress = { presses++ }) }
            assertTouchReaches(onNodeWithTag("centreEnd")) { assertEquals(1, presses, "the press reached the control") }
        }
    }

    // endregion

    // region assertScrollEndClearsBottomBar

    @Test
    fun `a list drawn edge to edge with no end padding fails wherever a bottom bar exists`() {
        val failed = failedOn {
            setContent { Rows(PaddingKind.None) }
            assertScrollEndClearsBottomBar(onNodeWithTag("rows"))
        }

        val withBottomBar = DeviceProfiles.all.filter { it.insets.bottom > 0 }.map { it.name }.toSet()
        assertEquals(withBottomBar, failed)
    }

    @Test
    fun `a list padded by the safe drawing inset ends clear of the bottom bar on every profile`() {
        matrix.forEachDeviceProfile(screen = "Fixture") {
            setContent { Rows(PaddingKind.SafeDrawing) }
            assertScrollEndClearsBottomBar(onNodeWithTag("rows"))
            assertNothingInteractiveUnderSystemBars()
        }
    }

    // endregion

    // region the window the runner builds is the profile

    @Test
    fun `the insets a screen reads under the Duo shape are the ones measured on the Duo`() {
        // From the Compose inset probe on the iPhone Duo simulator, iOS 27.2: every type that is not listed read 0.
        var read = emptyMap<String, SystemInsets>()
        matrix.forEachDeviceProfile(screen = "Fixture", profiles = listOf(DeviceProfiles.IPhoneDuoOuter)) {
            setContent { ProbeInsets { read = it } }
            rule.waitForIdle()
        }

        assertEquals(SystemInsets(), read.getValue("statusBars"))
        assertEquals(SystemInsets(bottom = 34), read.getValue("navigationBars"))
        assertEquals(SystemInsets(right = 84, bottom = 34), read.getValue("systemBars"))
        assertEquals(SystemInsets(), read.getValue("displayCutout"))
        assertEquals(SystemInsets(right = 84, bottom = 34), read.getValue("safeDrawing"))
        assertEquals(SystemInsets(right = 84, bottom = 34), read.getValue("safeGestures"))
        assertEquals(SystemInsets(right = 84, bottom = 34), read.getValue("safeContent"))
    }

    @Test
    fun `under the Android shape a side inset is the display cutout and not the system bars`() {
        var read = emptyMap<String, SystemInsets>()
        matrix.forEachDeviceProfile(screen = "Fixture", profiles = listOf(DeviceProfiles.PhoneLandscapeCutout)) {
            setContent { ProbeInsets { read = it } }
            rule.waitForIdle()
        }

        assertEquals(SystemInsets(left = 46), read.getValue("displayCutout"))
        assertEquals(SystemInsets(top = 28), read.getValue("systemBars"))
        assertEquals(SystemInsets(left = 46, top = 28), read.getValue("safeDrawing"))
    }

    @Test
    fun `a profile's font scale and density reach the composition`() {
        var fontScale = 0f
        var density = 0f
        matrix.forEachDeviceProfile(screen = "Fixture", profiles = listOf(DeviceProfiles.Narrow320Font2)) {
            setContent {
                ProbeDensity { measuredDensity, measuredFontScale ->
                    density = measuredDensity
                    fontScale = measuredFontScale
                }
            }
            rule.waitForIdle()
        }

        assertEquals(2.0f, fontScale)
        assertEquals(2.0f, density)
    }

    // endregion

    // region the table

    @Test
    fun `every named profile is in all, with a unique name and a source that says measured or assumed`() {
        val named = DeviceProfiles::class.java.declaredFields
            .filter { it.type == DeviceProfile::class.java }
            .map { it.apply { isAccessible = true }.get(DeviceProfiles) as DeviceProfile }

        assertEquals(named.map { it.name }.toSet(), DeviceProfiles.all.map { it.name }.toSet())
        assertEquals(DeviceProfiles.all.size, DeviceProfiles.all.map { it.name }.toSet().size)
        assertTrue(DeviceProfiles.all.all { it.source.startsWith("measured") || it.source.startsWith("assumed") })
    }

    @Test
    fun `a profile with no source is refused`() {
        assertFailsWith<IllegalArgumentException> {
            DeviceProfile(
                name = "Unlabelled",
                widthDp = 320,
                heightDp = 640,
                density = 2f,
                shape = PlatformInsetShape.ANDROID,
                source = "looks about right",
            )
        }
    }

    // endregion

    private fun failedOn(block: DeviceMatrixScope.(DeviceProfile) -> Unit): Set<String> {
        val error = assertFailsWith<AssertionError> { matrix.forEachDeviceProfile(screen = "Fixture", block = block) }
        return Regex("""^\[(\w+)]""", RegexOption.MULTILINE)
            .findAll(error.message.orEmpty())
            .map { it.groupValues[1] }
            .toSet()
    }
}

private enum class PaddingKind { None, SafeDrawing }

/** The screen incident: pads the sides by the display cutout only, the top by the status bar and the bottom by the navigation bar. */
@Composable
private fun Modifier.cutoutOnlyPadding(): Modifier = this
    .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
    .statusBarsPadding()
    .navigationBarsPadding()

/** A control in each corner and on the middle of each side, where a system bar stands. */
@Composable
private fun EdgeControls(modifier: Modifier, onPress: () -> Unit = {}) {
    Box(modifier = modifier.fillMaxSize()) {
        val places = listOf(
            "topStart" to Alignment.TopStart,
            "topEnd" to Alignment.TopEnd,
            "bottomStart" to Alignment.BottomStart,
            "bottomEnd" to Alignment.BottomEnd,
            "centreStart" to Alignment.CenterStart,
            "centreEnd" to Alignment.CenterEnd,
        )
        for ((tag, place) in places) {
            Box(modifier = Modifier.align(place).size(48.dp).testTag(tag).clickable(onClick = onPress))
        }
    }
}

@Composable
private fun Rows(padding: PaddingKind) {
    val contentPadding = when (padding) {
        PaddingKind.SafeDrawing -> WindowInsets.safeDrawing.asPaddingValues()
        PaddingKind.None -> PaddingValues()
    }
    LazyColumn(modifier = Modifier.fillMaxSize().testTag("rows"), contentPadding = contentPadding) {
        items(ROW_COUNT, key = { it }) { index ->
            BasicText(text = "Row $index", modifier = Modifier.size(width = 160.dp, height = 56.dp).clickable {})
        }
    }
}

private const val ROW_COUNT = 40

@Composable
private fun ProbeInsets(report: (Map<String, SystemInsets>) -> Unit) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    fun read(insets: WindowInsets) = with(density) {
        SystemInsets(
            left = insets.getLeft(density, direction).toDp().value.toInt(),
            top = insets.getTop(density).toDp().value.toInt(),
            right = insets.getRight(density, direction).toDp().value.toInt(),
            bottom = insets.getBottom(density).toDp().value.toInt(),
        )
    }
    val values = mapOf(
        "statusBars" to read(WindowInsets.statusBars),
        "navigationBars" to read(WindowInsets.navigationBars),
        "systemBars" to read(WindowInsets.systemBars),
        "displayCutout" to read(WindowInsets.displayCutout),
        "safeDrawing" to read(WindowInsets.safeDrawing),
        "safeGestures" to read(WindowInsets.safeGestures),
        "safeContent" to read(WindowInsets.safeContent),
    )
    SideEffect { report(values) }
    Box(modifier = Modifier.fillMaxSize().clickable {})
}

@Composable
private fun ProbeDensity(report: (Float, Float) -> Unit) {
    val density = LocalDensity.current
    SideEffect { report(density.density, density.fontScale) }
    Box(modifier = Modifier.fillMaxSize().clickable {})
}
