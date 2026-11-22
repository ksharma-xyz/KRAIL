package xyz.ksharma.krail.trip.planner.ui.intro

import xyz.ksharma.krail.trip.planner.ui.state.intro.IntroState
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * The rider's position goes from the location manager to the view model and nowhere else. The screen
 * takes it, passes it on in the same step, and keeps none of it: not in a remembered or saved value,
 * not in the state, not in a log line. A test can show the behaviour for the cases it drives, and
 * this holds the rule for every line a later change adds to the three files that touch the fix.
 */
class IntroLocationNeverHeldTest {

    private val glue: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, INTRO_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $INTRO_PATH above ${File("").absolutePath}")
        GLUE_FILES.map { File(dir, it) }
    }

    @Test
    fun `the files that touch the fix are found, so this guard cannot pass by looking at nothing`() {
        assertTrue(glue.all { it.isFile && it.readText().isNotBlank() }, "missing: ${glue.filterNot { it.isFile }}")
    }

    @Test
    fun `none of them writes the fix to a log or the console`() {
        val offenders = glue.flatMap { file ->
            file.codeLines().filter { line -> LOGGING.any { it in line } }.map { "${file.name}: $it" }
        }

        assertTrue(offenders.isEmpty(), "The fix must never be logged: $offenders")
    }

    @Test
    fun `none of them keeps a position in remembered, saved or observable state`() {
        val offenders = glue.flatMap { file ->
            file.codeLines()
                .filter { line -> HOLDERS.any { it in line } && POSITION_WORDS.any { it in line } }
                .map { "${file.name}: $it" }
        }

        assertTrue(offenders.isEmpty(), "The fix must never be held: $offenders")
    }

    @Test
    fun `the Intro state has no field that could carry a position`() {
        val types = IntroState::class.java.declaredFields.map { it.type.simpleName }

        assertTrue(types.none { it == "LatLng" || it == "Location" || it == "double" }, "fields: $types")
    }

    private fun File.codeLines(): List<String> = readLines()
        .map { it.trim() }
        .filterNot { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }

    private companion object {
        const val INTRO_PATH =
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro"
        val GLUE_FILES = listOf("IntroLocate.kt", "IntroLocationEffect.kt", "IntroFlowRoute.kt")
        val LOGGING = listOf("log(", "logError(", "println(", "print(", "Log.", "Logger", "Napier", "Kermit")
        val HOLDERS = listOf("remember", "mutableStateOf", "Saver", "SavedStateHandle", "MutableStateFlow", "var ")
        val POSITION_WORDS = listOf("LatLng", "Location>", "latitude", "longitude", "location:")
    }
}
