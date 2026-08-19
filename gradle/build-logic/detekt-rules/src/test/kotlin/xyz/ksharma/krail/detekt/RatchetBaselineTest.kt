package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers the ratchet mechanism itself — locating `config/<file>` by walking up from the analysed
 * file, keying entries on a repo-root-relative path, and spending a per-function allowance —
 * using [LazyItemKeyRule] as the carrier. A fake repository is built in a temp directory so the
 * paths are real; the rules' own tests cover detection with no baseline in play.
 */
class RatchetBaselineTest {

    @Test
    fun `suppresses exactly the grandfathered count and no more`() {
        val findings = lintInFakeRepo(
            baseline = "$SOURCE_RELATIVE_PATH|Screen|2",
            content = threeUnkeyedItems(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `suppresses every offender when the allowance covers them all`() {
        val findings = lintInFakeRepo(
            baseline = "$SOURCE_RELATIVE_PATH|Screen|3",
            content = threeUnkeyedItems(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `ignores comments and blank lines in the baseline`() {
        val findings = lintInFakeRepo(
            baseline = "# a comment\n\n$SOURCE_RELATIVE_PATH|Screen|3\n",
            content = threeUnkeyedItems(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `an entry for a different function grants no allowance`() {
        val findings = lintInFakeRepo(
            baseline = "$SOURCE_RELATIVE_PATH|SomeOtherScreen|3",
            content = threeUnkeyedItems(),
        )

        assertEquals(3, findings.size)
    }

    @Test
    fun `an entry for a different path grants no allowance`() {
        val findings = lintInFakeRepo(
            baseline = "other/module/src/commonMain/kotlin/Screen.kt|Screen|3",
            content = threeUnkeyedItems(),
        )

        assertEquals(3, findings.size)
    }

    private fun threeUnkeyedItems() = """
        @Composable
        fun Screen() {
            LazyColumn {
                item { Text("a") }
                item { Text("b") }
                item { Text("c") }
            }
        }
    """.trimIndent()

    private fun lintInFakeRepo(baseline: String, content: String): List<Finding> {
        val root = fakeRepo(LazyItemKeyRule.BASELINE_FILE, baseline)
        return LazyItemKeyRule(TestConfig()).lint(writeSource(root, content).toPath())
    }

    companion object {

        const val SOURCE_RELATIVE_PATH = "feature/sample/ui/src/commonMain/kotlin/Screen.kt"

        /** A throwaway repository root holding `config/<baselineFileName>`. Shared with the rules'
         * own tests, which need the same fixture to cover their baseline behaviour. */
        fun fakeRepo(baselineFileName: String, contents: String): File {
            val root = Files.createTempDirectory("krail-ratchet").toFile()
                .also { it.deleteOnExit() }
            val configDir = File(root, "config").also { it.mkdirs() }
            File(configDir, baselineFileName).writeText(contents)
            return root
        }

        fun writeSource(root: File, content: String): File =
            File(root, SOURCE_RELATIVE_PATH).apply {
                parentFile.mkdirs()
                writeText(content)
            }
    }
}
