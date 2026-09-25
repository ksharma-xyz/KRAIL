package xyz.ksharma.krail.detekt

import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Paths are fake absolute paths under `/krail`, never a real checkout, so no baseline file is
 * found and every file gets a zero allowance.
 */
class SystemDarkThemeBanTest {

    @Test
    fun `flags a read in a feature`() {
        val findings = lint(
            """
            import androidx.compose.foundation.isSystemInDarkTheme

            @Composable
            fun Tint() {
                val dark = isSystemInDarkTheme()
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
        assertTrue(findings.single().message.contains("isAppInDarkMode()"))
    }

    @Test
    fun `flags a fully qualified read`() {
        val findings = lint("val dark = androidx.compose.foundation.isSystemInDarkTheme()")

        assertEquals(1, findings.size)
    }

    @Test
    fun `flags every read in a file`() {
        val findings = lint(
            """
            fun a() = isSystemInDarkTheme()
            fun b() = isSystemInDarkTheme()
            """.trimIndent(),
        )

        assertEquals(2, findings.size)
    }

    @Test
    fun `allows the theme package, where ThemeMode is resolved`() {
        val findings = lint(
            """
            package xyz.ksharma.krail.taj.theme

            fun isAppInDarkMode(): Boolean = isSystemInDarkTheme()
            """.trimIndent(),
            filename = "/krail/taj/src/commonMain/kotlin/xyz/ksharma/krail/taj/theme/ThemeManager.kt",
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag isAppInDarkMode`() {
        val findings = lint("fun a() = isAppInDarkMode()")

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag commented-out code`() {
        val findings = lint(
            """
            /*
            val dark = isSystemInDarkTheme()
            */
            fun a() = Unit
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag test sources`() {
        val findings = lint(
            "fun a() = isSystemInDarkTheme()",
            filename = "/krail/feature/x/src/commonTest/kotlin/ThemeTest.kt",
        )

        assertEquals(0, findings.size)
    }

    private fun lint(
        code: String,
        filename: String = "/krail/feature/x/src/commonMain/kotlin/Prod.kt",
    ) = SystemDarkThemeBan(TestConfig()).lint(compileContentForTest(code, filename = filename))
}
