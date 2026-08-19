package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnackbarBanTest {

    private val rule = SnackbarBan(TestConfig())

    @Test
    fun `flags a Snackbar composable call`() {
        val code = """
            @Composable
            fun Screen() {
                Snackbar { Text("Saved") }
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags a SnackbarHost and its host state`() {
        val code = """
            @Composable
            fun Screen() {
                val hostState = remember { SnackbarHostState() }
                SnackbarHost(hostState)
            }
        """.trimIndent()

        assertEquals(2, rule.lint(code).size)
    }

    @Test
    fun `flags a showSnackbar call`() {
        val code = """
            suspend fun notify(hostState: Any) {
                hostState.showSnackbar("Trip removed")
            }
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `flags the import`() {
        val code = """
            import androidx.compose.material3.SnackbarHost

            fun nothing() = Unit
        """.trimIndent()

        assertEquals(1, rule.lint(code).size)
    }

    @Test
    fun `does not flag a ModalBottomSheet confirmation`() {
        val code = """
            @Composable
            fun Screen() {
                ModalBottomSheet(onDismissRequest = {}) { Text("Delete this trip?") }
            }
        """.trimIndent()

        assertEquals(0, rule.lint(code).size)
    }

    @Test
    fun `message offers the sanctioned alternatives`() {
        val code = """
            @Composable
            fun Screen() {
                Snackbar { Text("Saved") }
            }
        """.trimIndent()

        val message = rule.lint(code).single().message

        assertTrue(message.contains("edit mode"), message)
        assertTrue(message.contains("ModalBottomSheet"), message)
    }
}
