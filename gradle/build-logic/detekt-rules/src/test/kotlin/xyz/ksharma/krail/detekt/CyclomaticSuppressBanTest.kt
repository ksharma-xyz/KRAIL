package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CyclomaticSuppressBanTest {

    private val rule = CyclomaticSuppressBan(TestConfig())

    @Test
    fun `flags a function-level suppression`() {
        val code = """
            @Suppress("CyclomaticComplexMethod")
            fun decide(input: Int): String = if (input > 0) "a" else "b"
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags a file-level suppression`() {
        val code = """
            @file:Suppress("CyclomaticComplexMethod")

            package xyz.ksharma.krail.sample

            fun decide(input: Int): String = "a"
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags it when hidden among other suppressed rules`() {
        val code = """
            @file:Suppress(
                "TooManyFunctions",
                "LongMethod",
                "CyclomaticComplexMethod",
            )

            package xyz.ksharma.krail.sample

            fun decide(input: Int): String = "a"
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag other suppressions`() {
        val code = """
            @Suppress("LongMethod", "TooManyFunctions")
            fun decide(input: Int): String = "a"
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag an unsuppressed complex function`() {
        val code = """
            fun decide(input: Int): String = when {
                input > 10 -> "big"
                input > 5 -> "medium"
                else -> "small"
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `grandfathers a file-level suppression listed in the closed baseline`() {
        val root = RatchetBaselineTest.fakeRepo(
            CyclomaticSuppressBan.BASELINE_FILE,
            "${RatchetBaselineTest.SOURCE_RELATIVE_PATH}|(file)",
        )
        val source = RatchetBaselineTest.writeSource(root, FILE_SUPPRESSED_SOURCE)

        assertEquals(0, rule.lint(source.toPath()).size)
    }

    @Test
    fun `still flags a suppression the baseline does not list`() {
        val root = RatchetBaselineTest.fakeRepo(
            CyclomaticSuppressBan.BASELINE_FILE,
            "some/other/File.kt|(file)",
        )
        val source = RatchetBaselineTest.writeSource(root, FILE_SUPPRESSED_SOURCE)

        assertEquals(1, rule.lint(source.toPath()).size)
    }

    @Test
    fun `message says to refactor and warns that a local fun does not help`() {
        val code = """
            @Suppress("CyclomaticComplexMethod")
            fun decide(input: Int): String = "a"
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("zero tolerance"), message)
        assertTrue(message.contains("local 'fun' does not reduce the count"), message)
    }

    private companion object {
        val FILE_SUPPRESSED_SOURCE = """
            @file:Suppress("CyclomaticComplexMethod")

            package sample

            fun decide(input: Int): String = "a"
        """.trimIndent()
    }
}
