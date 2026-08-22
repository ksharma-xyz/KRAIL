package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeColorRoleRuleTest {

    private val rule = ThemeColorRoleRule(TestConfig())

    @Test
    fun `flags the deprecated accessor`() {
        val code = """
            @Composable
            fun Title() {
                Text(text = "KRAIL", color = themeColor())
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the deprecated accessor's import`() {
        val code = """
            import xyz.ksharma.krail.taj.themeColor

            fun nothing() = Unit
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the ground colour passed as a text colour`() {
        val code = """
            @Composable
            fun Title() {
                Text(text = "KRAIL", color = themeGroundColor())
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the ground colour passed as an icon tint`() {
        val code = """
            @Composable
            fun Chevron() {
                Icon(painter = painter, tint = themeGroundColor())
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the ground colour inside a BorderStroke`() {
        val code = """
            @Composable
            fun Chip() {
                Box(modifier = Modifier.border(BorderStroke(1.dp, themeGroundColor())))
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag the ground colour used as a fill`() {
        val code = """
            @Composable
            fun Badge() {
                TransportModeBadge(backgroundColor = themeGroundColor())
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    /**
     * `Modifier.background(color = ...)` is a fill with content drawn on top, so the ground
     * accessor is exactly right there. It names its argument `color`, the same as
     * `Modifier.border()`, which takes ink — so the callee has to be part of the decision. This
     * shape is real: DiscoverChip's selected state, which the rule flagged wrongly at first.
     */
    @Test
    fun `does not flag the ground colour in a background fill`() {
        val code = """
            @Composable
            fun Chip(selected: Boolean) {
                Box(
                    modifier = Modifier.background(
                        color = if (selected) themeGroundColor() else KrailTheme.colors.surface,
                        shape = RoundedCornerShape(8.dp),
                    ),
                )
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    /** The same argument name on a stroke still has to be caught. */
    @Test
    fun `still flags the ground colour in a border stroke`() {
        val code = """
            @Composable
            fun Chip() {
                Box(modifier = Modifier.border(width = 1.dp, color = themeGroundColor()))
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag the ink accessor in an ink position`() {
        val code = """
            @Composable
            fun Title() {
                Text(text = "KRAIL", color = themeInkColor())
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag the decoration accessor, which is the sanctioned way out`() {
        val code = """
            @Composable
            fun Row() {
                Box(modifier = Modifier.klickable(indication = krailRipple(color = themeDecorColor())))
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    /**
     * `Klickable.kt` declares `indication: Indication? = krailRipple(color = themeDecorColor())`
     * as a **default argument**, so the decoration accessor appears in a literal `color =`
     * position in the one file every clickable surface in the app routes through. Confirming it
     * does not trip, because a false positive there would be unavoidable rather than fixable.
     */
    @Test
    fun `does not flag the klickable default argument`() {
        val code = """
            @Composable
            fun Klickable(
                indication: Indication? = krailRipple(color = themeDecorColor()),
            ) = Unit
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag the wash accessor`() {
        val code = """
            @Composable
            fun Card() {
                Box(modifier = Modifier.background(color = themeBackgroundColor()))
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag an unrelated colour in an ink position`() {
        val code = """
            @Composable
            fun Title() {
                Text(text = "Park & Ride", color = KrailTheme.colors.label)
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `the deprecated message names all four roles`() {
        val code = """
            @Composable
            fun Title() = Text(color = themeColor())
        """.trimIndent()

        val message = rule.lint(code).single().message

        listOf("themeGroundColor", "themeInkColor", "themeBackgroundColor", "themeDecorColor")
            .forEach { assertTrue(message.contains(it), "message should mention $it: $message") }
    }

    @Test
    fun `the ink message offers themeInkColorOn for a non-surface ground`() {
        val code = """
            @Composable
            fun Hand() = Icon(tint = themeGroundColor())
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("themeInkColorOn"), message)
        assertTrue(message.contains("themeDecorColor"), message)
    }
}
