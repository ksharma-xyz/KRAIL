package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LazyItemKeyRuleTest {

    private val rule = LazyItemKeyRule(TestConfig())

    @Test
    fun `flags an unkeyed item inside a LazyColumn`() {
        val code = """
            @Composable
            fun Screen() {
                LazyColumn {
                    item {
                        Text("hello")
                    }
                }
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags every unkeyed item in the same list`() {
        val code = """
            @Composable
            fun Screen() {
                LazyRow {
                    item { Text("a") }
                    item(key = "b") { Text("b") }
                    item { Text("c") }
                }
            }
        """.trimIndent()

        assertEquals(2, rule.lint(code).size)
    }

    @Test
    fun `flags an unkeyed item in a LazyListScope extension function`() {
        val code = """
            private fun LazyListScope.recentStopsSection() {
                item {
                    Text("Recent")
                }
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags an unkeyed item in a fully-qualified LazyListScope extension function`() {
        val code = """
            private fun androidx.compose.foundation.lazy.LazyListScope.recentStopsSection() {
                item {
                    Text("Recent")
                }
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag a receiver whose name merely contains a lazy scope name`() {
        // The scope check is an exact simple-name match, not containment: this receiver is an
        // unrelated type that happens to embed "LazyListScope" in its name, and `item` on it is
        // somebody else's function with somebody else's semantics.
        val code = """
            private fun MyLazyListScopeExtension.render() {
                item {
                    Text("not a lazy list")
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag a positional string key`() {
        val code = """
            @Composable
            fun Screen() {
                LazyColumn {
                    item("spacer-top") { Spacer(Modifier.height(8.dp)) }
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag a named key argument`() {
        val code = """
            @Composable
            fun Screen() {
                LazyVerticalGrid(columns = GridCells.Fixed(2)) {
                    item(key = "load-more-button") { LoadMoreButton() }
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag a keyed items call`() {
        val code = """
            @Composable
            fun Screen(journeys: List<Journey>) {
                LazyColumn {
                    items(journeys, key = { it.journeyId }) { Text(it.name) }
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag an unkeyed items call - out of scope for now`() {
        // Documented limitation: items(...) detection is a follow-up, see the rule's KDoc.
        val code = """
            @Composable
            fun Screen(journeys: List<Journey>) {
                LazyColumn {
                    items(journeys) { Text(it.name) }
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag an unrelated function called item`() {
        val code = """
            fun build(menu: Menu) {
                menu.item {
                    label = "Settings"
                }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `message explains the positional-identity failure`() {
        val code = """
            @Composable
            fun Screen() {
                LazyColumn {
                    item { Text("hello") }
                }
            }
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("by position"), message)
        assertTrue(message.contains("CLAUDE.md"), message)
    }
}
