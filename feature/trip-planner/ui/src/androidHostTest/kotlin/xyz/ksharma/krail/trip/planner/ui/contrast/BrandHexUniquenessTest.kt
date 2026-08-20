package xyz.ksharma.krail.trip.planner.ui.contrast

import xyz.ksharma.krail.core.transport.TransportMode
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * `:core:transport` owns the NSW mode brand colours. No other production file may write one as a
 * literal.
 *
 * A copied hex is the drift that nobody notices: when a mode colour changes, the badge on the
 * saved-trip card updates and the preview two modules away keeps the old orange, so screenshots
 * disagree with the app and the first person to look assumes the screenshot is right. The fix is
 * always to reference `TransportMode.<Mode>.colorCode` or the matching `taj` theme token instead
 * of retyping the value.
 *
 * ## Scope
 *
 * Production main source sets only (commonMain, androidMain, iosMain) — test fixtures and baselines
 * are excluded, since a test asserting on a specific colour is meant to name it. Matching covers
 * both spellings a Kotlin file can use, `"#RRGGBB"` and `Color(0xFFRRGGBB)`, in either case.
 *
 * [ALLOWED] is a shrinking list of files that legitimately (or historically) restate a brand
 * colour, each with its reason. It fails the build for any file not on it, so new drift cannot
 * start. Two entries are genuinely unavoidable: `:taj` is a leaf design system that must not
 * depend on `:core:transport`, so its theme and gradient tokens have to carry their own copies.
 * The rest are preview fixtures that should move to `TransportMode.<Mode>.colorCode` — delete the
 * entry as each one does.
 */
class BrandHexUniquenessTest {

    @Test
    fun `no production file outside core transport restates a mode brand colour`() {
        val root = repoRoot()
        val brandHexes = TransportMode.all
            .map { it.colorCode.removePrefix("#").uppercase() }
            .toSet()
        val pattern = Regex(
            "(?:#|0x[fF]{2})(" + brandHexes.joinToString("|") + ")",
            RegexOption.IGNORE_CASE,
        )

        val offenders = productionSources(root)
            .map { it.toRelativeString(root).replace(File.separatorChar, '/') }
            .filterNot { it.startsWith(OWNING_MODULE) }
            .filter { pattern.containsMatchIn(File(root, it).readText()) }
            .toSet()

        val unexpected = offenders - ALLOWED.keys
        if (unexpected.isNotEmpty()) {
            fail(
                buildString {
                    appendLine("These files restate a TransportMode brand colour as a literal:")
                    unexpected.sorted().forEach { appendLine("  - $it") }
                    appendLine(
                        "Reference TransportMode.<Mode>.colorCode (or the matching taj theme " +
                            "token where the module cannot depend on :core:transport) instead " +
                            "of retyping the hex.",
                    )
                },
            )
        }

        val stale = ALLOWED.keys - offenders
        assertTrue(
            stale.isEmpty(),
            "${stale.joinToString()} no longer contains a brand hex literal — delete its " +
                "ALLOWED entry so the list keeps shrinking.",
        )
    }

    private fun productionSources(root: File): Sequence<File> = root.walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRECTORIES }
        .filter { it.isFile && it.extension == "kt" }
        .filter { file ->
            val path = file.invariantSeparatorsPath
            PRODUCTION_SOURCE_SET.containsMatchIn(path)
        }

    /** Walk up from the module's working directory to the directory owning the Gradle build. */
    private fun repoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null) {
            if (File(dir, "settings.gradle.kts").isFile) return dir
            dir = dir.parentFile
        }
        error("Could not locate the repository root from ${File("").absolutePath}")
    }

    private companion object {

        const val OWNING_MODULE = "core/transport/"

        val PRODUCTION_SOURCE_SET = Regex("/src/[a-zA-Z]+Main/kotlin/")

        val SKIPPED_DIRECTORIES = setOf("build", ".git", ".gradle", ".kotlin", ".claude")

        /** Files that restate a brand colour, and why. Delete an entry as each is fixed. */
        val ALLOWED = mapOf(
            // Unavoidable: :taj is a leaf design system with no project dependencies, so it
            // cannot read TransportMode. These three are the source of truth on the taj side and
            // must be updated together with TransportMode if a brand colour ever changes.
            "taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/theme/Color.kt" to
                "taj cannot depend on :core:transport; these are the theme tokens themselves.",
            "taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/tokens/AiGradientTokens.kt" to
                "Same: the AI gradient's stops are mode colours, declared taj-side.",
            "taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/tokens/AiThemeGradientTokens.kt" to
                "Same, for the per-theme variant of that gradient.",
            // Preview and sample fixtures. Each should take the colour from TransportMode.
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "components/DepartureBoardStopCard.kt" to "Preview fixture.",
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "components/DepartureRow.kt" to
                "Preview fixtures plus a private BUS_COLOR constant.",
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "components/LinesServedRow.kt" to "Preview fixtures.",
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "components/TransportModeBadge.kt" to "Preview fixture.",
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "journeymap/JourneyStopDetailsBottomSheet.kt" to "Preview fixtures.",
            "feature/trip-planner/ui/src/commonMain/kotlin/xyz/ksharma/krail/trip/planner/ui/" +
                "tracktrip/TrackTripScreen.kt" to "Preview fixture.",
        )
    }
}
