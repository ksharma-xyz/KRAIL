package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * These use `lint` rather than `compileAndLint`: the snippets reference Compose and KRAIL
 * annotations (`@Preview`, `@Composable`, `@ScreenshotTest`, `@PreviewComponent`) that are not on
 * this module's test classpath, and the rule matches on the annotation's short name anyway.
 */
class ScreenshotPreviewAnnotationTest {

    private val rule = ScreenshotPreviewAnnotation(TestConfig())

    @Test
    fun `flags a ScreenshotTest paired with a bare Preview`() {
        val code = """
            @ScreenshotTest
            @Preview
            @Composable
            private fun MyThingPreview() {}
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags a ScreenshotTest paired with a parameterised bare Preview`() {
        val code = """
            @ScreenshotTest(threshold = 0.01)
            @Preview(group = "Widgets", showBackground = true)
            @Composable
            private fun MyThingPreview() {}
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags a ScreenshotTest with no preview annotation at all`() {
        val code = """
            @ScreenshotTest
            @Composable
            private fun MyThingPreview() {}
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag PreviewComponent`() {
        val code = """
            @ScreenshotTest
            @PreviewComponent
            @Composable
            private fun MyThingPreview() {}
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag PreviewScreen regardless of annotation order`() {
        val code = """
            @PreviewScreen
            @ScreenshotTest
            @Composable
            private fun MyScreenPreview() {}
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag a bare Preview that is not a screenshot test`() {
        val code = """
            @Preview
            @Composable
            private fun MyThingPreview() {}
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `message names the function and points at the project convention`() {
        val code = """
            @ScreenshotTest
            @Preview
            @Composable
            private fun DividerHorizontalPreview() {}
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("DividerHorizontalPreview"), message)
        assertTrue(message.contains("@PreviewComponent"), message)
        assertTrue(message.contains("CLAUDE.md"), message)
    }
}
