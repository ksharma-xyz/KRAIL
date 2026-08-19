package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CollectAsStateBanTest {

    private val rule = CollectAsStateBan(TestConfig())

    @Test
    fun `flags a collectAsState call`() {
        val code = """
            @Composable
            fun Screen(viewModel: ScreenViewModel) {
                val state by viewModel.uiState.collectAsState()
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags a collectAsState call with an initial value`() {
        val code = """
            @Composable
            fun Screen(viewModel: ScreenViewModel) {
                val state by viewModel.events.collectAsState(initial = null)
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the import even with no call site left`() {
        val code = """
            import androidx.compose.runtime.collectAsState

            fun nothing() = Unit
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag collectAsStateWithLifecycle`() {
        val code = """
            import androidx.lifecycle.compose.collectAsStateWithLifecycle

            @Composable
            fun Screen(viewModel: ScreenViewModel) {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `does not flag an unrelated collect`() {
        val code = """
            suspend fun observe(flow: Flow<Int>) {
                flow.collect { }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `message points at the polling lifecycle doc`() {
        val code = """
            @Composable
            fun Screen(viewModel: ScreenViewModel) {
                val state by viewModel.uiState.collectAsState()
            }
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("collectAsStateWithLifecycle"), message)
        assertTrue(message.contains("docs/POLLING_LIFECYCLE.md"), message)
    }
}
