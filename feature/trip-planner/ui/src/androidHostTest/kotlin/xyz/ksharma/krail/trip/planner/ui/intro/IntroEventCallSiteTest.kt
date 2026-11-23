package xyz.ksharma.krail.trip.planner.ui.intro

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `intro_lets_krail` is built in one place, `IntroOutcome.toEvent()`, and that call sets `introVersion` from an outcome
 * whose version is not optional. This proves no second place builds it, which would be a firing that could leave the
 * version out. It reads source files off disk, which commonTest cannot do.
 *
 * It sees every `IntroLetsKrailClickEvent(` in production Kotlin. It cannot see an event built through a function
 * that wraps the class, so a new wrapper would have to be added here by whoever writes it.
 */
class IntroEventCallSiteTest {

    @Test
    fun `the Intro outcome event is built in one place only`() {
        val builders = productionSources()
            .filter { file -> CALL.containsMatchIn(file.readText()) }
            .map { it.relativeTo(repoRoot).path.replace('\\', '/') }
            .toList()
            .sorted()

        assertEquals(
            listOf(
                "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro/" +
                    "IntroOutcomeEvent.kt",
            ),
            builders,
            "intro_lets_krail must be built only by IntroOutcome.toEvent(), which always sends introVersion",
        )
    }

    @Test
    fun `the one call sets the version from the outcome`() {
        val file = productionSources().firstOrNull { it.name == "IntroOutcomeEvent.kt" }
            ?: fail("IntroOutcomeEvent.kt not found")
        val call = file.readText().substringAfter("IntroLetsKrailClickEvent(").substringBefore("\n)")

        assertTrue("introVersion = introVersion," in call, "the call does not pass the outcome's version: $call")
        assertTrue("themeId = themeId" in call, "the call does not pass the chosen theme: $call")
    }

    private fun productionSources(): Sequence<File> =
        repoRoot.walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val path = file.path.replace('\\', '/')
                PRODUCTION_SOURCE_SETS.any { path.contains(it) }
            }

    private companion object {
        // A construction, not the class declaration or an import or a mention in a comment.
        val CALL = Regex("""(?m)^(?!\s*(?:\*|//|import|data class)).*\bIntroLetsKrailClickEvent\(""")

        val PRODUCTION_SOURCE_SETS =
            listOf("/src/commonMain/", "/src/androidMain/", "/src/iosMain/")

        val repoRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").isFile }
            ?: error("Could not locate the repo root from ${File(".").absolutePath}")
    }
}
