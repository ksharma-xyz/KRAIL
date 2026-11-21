package xyz.ksharma.krail.trip.planner.ui.intro

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Add to home in the Intro must never arm the app review moment. The picker arms it when a rider adds
 * a car park, and it fires when they next land on Saved Trips, which for a rider in their first minute
 * is the Intro's own exit. The Intro has no review manager to call, so this holds that nobody gives
 * it one: it fails if any Intro source names the review manager or a delight moment.
 */
class IntroNeverReviewsTest {

    private val introSources: List<File> by lazy {
        val dir = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, INTRO_V2_PATH) }
            .firstOrNull { it.isDirectory }
            ?: error("Cannot find $INTRO_V2_PATH above ${File("").absolutePath}")
        dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    @Test
    fun `the Intro sources are found, so this guard cannot pass by looking at nothing`() {
        assertTrue(introSources.size >= MIN_SOURCES, "found only ${introSources.map { it.name }}")
    }

    @Test
    fun `no Intro source names the review manager or a delight moment`() {
        val offenders = introSources.flatMap { file ->
            FORBIDDEN.filter { it in file.readText() }.map { "${file.name} names $it" }
        }

        assertTrue(offenders.isEmpty(), "The Intro must never ask for a review: $offenders")
    }

    private companion object {
        const val INTRO_V2_PATH =
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/intro/v2"
        const val MIN_SOURCES = 6
        val FORBIDDEN = listOf("AppReviewManager", "DelightMoment", "onDelightMoment", "PARK_RIDE_ADDED")
    }
}
