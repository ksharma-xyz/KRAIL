package xyz.ksharma.krail.core.testing.device

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import org.robolectric.RuntimeEnvironment

/**
 * Runs a screen on every [DeviceProfile], each on a window of its own size, density and font scale with that platform's real
 * system insets dispatched to it and a node over every system region that takes the touch. `docs/testing/DEVICE_PROFILES.md`
 * is the spec and has the ten line example.
 *
 * ```
 * @get:Rule val matrix = DeviceMatrixRule()
 *
 * @Test fun `every control stands inside the safe area`() =
 *     matrix.forEachDeviceProfile(screen = "SettingsScreen") { profile ->
 *         setContent(theme = { BareKrailTheme(it) }) { SettingsScreen(...) }
 *         assertNothingInteractiveUnderSystemBars()
 *     }
 * ```
 *
 * The test class must run under Robolectric with the native graphics mode, as every Compose host test here does.
 */
class DeviceMatrixRule : TestRule {
    private val composeRule = createEmptyComposeRule()

    override fun apply(base: Statement, description: Description): Statement = composeRule.apply(base, description)

    /**
     * Runs [block] once for each of [profiles], on a fresh activity each time, and fails once at the end naming every profile
     * that failed, with its numbers and where they came from, so one run shows the whole matrix and not the first red cell.
     *
     * [screen] is the composable's name, for example `SearchStopScreen`. It is what the adoption guard reads to know the screen
     * has a matrix test, so it must be the real name.
     */
    fun forEachDeviceProfile(
        screen: String,
        profiles: List<DeviceProfile> = DeviceProfiles.all,
        block: DeviceMatrixScope.(DeviceProfile) -> Unit,
    ) {
        require(screen.isNotBlank()) { "Name the screen the matrix runs on, for example screen = \"SettingsScreen\"" }
        require(profiles.isNotEmpty()) { "A matrix test with no profiles checks nothing" }
        val failures = profiles.mapNotNull { profile ->
            runCatching { runOn(profile, block) }.exceptionOrNull()?.let { profile to it }
        }
        if (failures.isNotEmpty()) {
            throw AssertionError(
                buildString {
                    appendLine("$screen failed on ${failures.size} of ${profiles.size} device profiles.")
                    failures.forEach { (profile, problem) ->
                        appendLine()
                        appendLine("[${profile.name}] ${profile.describe()}")
                        appendLine("  ${problem.message?.replace("\n", "\n  ")}")
                    }
                },
                failures.first().second,
            )
        }
    }

    private fun runOn(profile: DeviceProfile, block: DeviceMatrixScope.(DeviceProfile) -> Unit) {
        // Before the activity exists: its window takes its size, density and font scale from these as it starts.
        RuntimeEnvironment.setQualifiers(profile.qualifiers())
        RuntimeEnvironment.setFontScale(profile.fontScale)
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            DeviceMatrixScope(profile, composeRule, scenario).block(profile)
        }
    }
}
