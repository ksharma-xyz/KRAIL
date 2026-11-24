package xyz.ksharma.krail.core.testing.device

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Every screen is run on the device profile matrix, or is on a list that only shrinks.
 *
 * A screen is a top level `@Composable fun XxxScreen(` in a `ui/src/commonMain` source set. It is covered when a host test
 * calls `forEachDeviceProfile(screen = "XxxScreen")`, which is what runs it on every profile in
 * `docs/testing/DEVICE_PROFILES.md`. The Duo side inset incident was a screen nobody ran on a device with a side inset, so a new
 * screen that skips the matrix is the mistake this exists to stop.
 *
 * The baseline, [BASELINE_PATH], holds the screens that have no matrix test yet, each with its reason, and follows the ratchet
 * contract in `docs/testing/GUARDS.md`: it only shrinks, every entry has a reason, and an entry is a failure once its screen
 * has a matrix test or no longer exists. Adding a line to make a new screen pass is not what the file is for.
 *
 * It is a text scan, stated plainly: it reads source, not the compiled tree. It cannot tell that a matrix test also runs the
 * assertions that matter, only that the screen is named to the runner, so the review of a matrix test is still a review.
 */
class DeviceMatrixAdoptionGuardTest {

    @Test
    fun `every screen has a device matrix test or a reason on the baseline`() {
        val root = repoRoot()
        val screens = screensIn(root)
        val covered = matrixScreensIn(root)
        val baseline = readBaseline(root)

        // A scan that finds nothing would pass for any tree, so it fails loudly instead.
        assertTrue(screens.size >= MIN_SCREENS, "found only ${screens.size} screens, so this guard scans nothing")
        assertTrue(covered.isNotEmpty(), "found no forEachDeviceProfile(screen = ...) call, so this guard scans nothing")

        val problems = adoptionProblems(screens, covered, baseline)
        if (problems.isNotEmpty()) fail(problems.joinToString("\n\n"))
    }

    // region the check itself goes red

    @Test
    fun `a screen with no matrix test and no baseline entry fails and says what to do`() {
        val problems = adoptionProblems(
            screens = setOf("NewScreen", "OtherScreen"),
            covered = setOf("OtherScreen"),
            baseline = emptyMap(),
        )

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("NewScreen has no device matrix test"), problems.single())
        assertTrue(problems.single().contains("forEachDeviceProfile(screen = \"NewScreen\")"), problems.single())
    }

    @Test
    fun `a baseline entry whose screen now has a matrix test is stale and fails`() {
        val problems = adoptionProblems(
            screens = setOf("DoneScreen"),
            covered = setOf("DoneScreen"),
            baseline = mapOf("DoneScreen" to "was not adopted yet"),
        )

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("DoneScreen is on the baseline but has a device matrix test"), problems.single())
    }

    @Test
    fun `a baseline entry whose screen no longer exists is stale and fails`() {
        val problems = adoptionProblems(
            screens = setOf("RealScreen"),
            covered = setOf("RealScreen"),
            baseline = mapOf("GoneScreen" to "was deleted"),
        )

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("GoneScreen is on the baseline but is not a screen"), problems.single())
    }

    @Test
    fun `a baseline entry with no reason fails`() {
        val problems = adoptionProblems(
            screens = setOf("SilentScreen"),
            covered = emptySet(),
            baseline = mapOf("SilentScreen" to ""),
        )

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("SilentScreen is on the baseline with no reason"), problems.single())
    }

    @Test
    fun `a matrix test naming a screen that does not exist fails`() {
        val problems = adoptionProblems(
            screens = setOf("RealScreen"),
            covered = setOf("RealScreen", "TypoScreen"),
            baseline = emptyMap(),
        )

        assertEquals(1, problems.size, problems.toString())
        assertTrue(problems.single().contains("names TypoScreen"), problems.single())
    }

    @Test
    fun `a matrix test naming a component that is not a screen is left alone`() {
        val problems = adoptionProblems(
            screens = setOf("RealScreen"),
            covered = setOf("RealScreen", "StopCard"),
            baseline = emptyMap(),
        )

        assertEquals(emptyList(), problems)
    }

    @Test
    fun `only a top level public or internal composable ending in Screen counts as a screen`() {
        val source = """
            package demo

            @Composable
            fun PlainScreen(modifier: Modifier = Modifier) {}

            @OptIn(ExperimentalComposeUiApi::class)
            @Composable
            internal fun InternalScreen() {}

            @Composable
            private fun PrivateScreen() {}

            @Composable
            fun ScreenContent() {}

            @Composable
            fun DiscoverScreenCompact() {}

            fun NotComposableScreen() {}

            class Holder {
                @Composable
                fun NestedScreen() {}
            }

            // @Composable
            // fun CommentedScreen() {}
        """.trimIndent()

        assertEquals(setOf("PlainScreen", "InternalScreen"), screenNamesIn(source))
    }

    @Test
    fun `a matrix call names its screen however the arguments are ordered`() {
        val source = """
            matrix.forEachDeviceProfile(screen = "FirstScreen") { setContent { } }
            matrix.forEachDeviceProfile(
                profiles = listOf(DeviceProfiles.SmallPhone),
                screen = "SecondScreen",
            ) { }
            // matrix.forEachDeviceProfile(screen = "CommentedScreen") { }
        """.trimIndent()

        assertEquals(setOf("FirstScreen", "SecondScreen"), matrixScreenNamesIn(source))
    }

    // endregion

    private fun adoptionProblems(
        screens: Set<String>,
        covered: Set<String>,
        baseline: Map<String, String>,
    ): List<String> = buildList {
        screens.filter { it !in covered && it !in baseline }.sorted().forEach { screen ->
            add(
                "$screen has no device matrix test. Add one that calls " +
                    "forEachDeviceProfile(screen = \"$screen\") (docs/testing/DEVICE_PROFILES.md). Putting the name on " +
                    "$BASELINE_PATH is not the fix: that list only shrinks.",
            )
        }
        baseline.forEach { (screen, reason) ->
            when {
                screen !in screens ->
                    add("$screen is on the baseline but is not a screen any more. Delete its line from $BASELINE_PATH.")
                screen in covered ->
                    add("$screen is on the baseline but has a device matrix test now. Delete its line from $BASELINE_PATH.")
                reason.isBlank() ->
                    add("$screen is on the baseline with no reason. Every entry says why, after a #, in $BASELINE_PATH.")
            }
        }
        covered.filter { it.endsWith("Screen") && it !in screens }.sorted().forEach { name ->
            add("A device matrix test names $name, which is not a screen in a ui/src/commonMain source set. Fix the name.")
        }
    }

    private fun screensIn(root: File): Set<String> = root.kotlinFiles { it.contains("/ui/src/commonMain/") }
        .flatMap { screenNamesIn(it.readText()) }
        .toSet()

    private fun matrixScreensIn(root: File): Set<String> = root.kotlinFiles {
        it.contains("/src/androidHostTest/") && !it.startsWith("core/testing/")
    }.flatMap { matrixScreenNamesIn(it.readText()) }.toSet()

    private fun screenNamesIn(source: String): Set<String> {
        val code = source.withoutComments()
        return SCREEN.findAll(code)
            .filter { match -> "@Composable" in match.groupValues[1] }
            .map { it.groupValues[2] }
            .toSet()
    }

    private fun matrixScreenNamesIn(source: String): Set<String> =
        MATRIX_CALL.findAll(source.withoutComments()).map { it.groupValues[1] }.toSet()

    private fun String.withoutComments(): String = replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    /** `<screen name>` to `<reason>`, with `#` comments and blank lines dropped. */
    private fun readBaseline(root: File): Map<String, String> {
        val file = File(root, BASELINE_PATH)
        if (!file.exists()) return emptyMap()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .associate { line -> line.substringBefore('#').trim() to line.substringAfter('#', "").trim() }
    }

    private fun File.kotlinFiles(pathFilter: (String) -> Boolean): Sequence<File> {
        val root = this
        return walkTopDown()
            .onEnter { it.name != "build" && it.name != ".git" && it.name != ".worktrees" }
            .filter { it.isFile && it.extension == "kt" }
            .filter { pathFilter(it.relativeTo(root).invariantPath()) }
    }

    /** Anchored on `settings.gradle.kts` rather than a count of `..` hops, so moving the module cannot make this scan nothing. */
    private fun repoRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
        }
        fail("Could not find the repo root (no settings.gradle.kts above ${File(".").absolutePath})")
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private companion object {
        const val BASELINE_PATH = "config/device-matrix-baseline.txt"
        const val MIN_SCREENS = 10

        // Column zero: a top level function. A member or a nested one is indented. Group 1 is the annotations directly above it,
        // so only a function that is itself marked `@Composable` counts, not one that follows a composable.
        val SCREEN = Regex(
            """(?m)^((?:@[\w.]+(?:\([^)]*\))?[ \t]*\n)*)(?:internal |public )?fun ([A-Z]\w*Screen)\(""",
        )
        val MATRIX_CALL = Regex("""forEachDeviceProfile\([^"]*?screen\s*=\s*"(\w+)"""", RegexOption.DOT_MATCHES_ALL)
        val BLOCK_COMMENT = Regex("""/\*.*?\*/""", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("""//[^\n]*""")
    }
}
