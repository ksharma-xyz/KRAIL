package xyz.ksharma.krail.detekt

import io.github.detekt.test.utils.compileContentForTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Paths here are fake absolute paths under `/krail`, never a real checkout, so no
 * `config/clock-system-baseline.txt` is ever found and every file gets a zero allowance. That
 * keeps these tests independent of whatever the real baseline currently holds.
 */
class ClockSystemBanTest {

    @Test
    fun `flags an inline read in a function body`() {
        val findings = lintProduction(
            """
            fun stale(at: Instant): Boolean = at < Clock.System.now()
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
        assertTrue(findings.single().message.contains("injected Clock"))
    }

    @Test
    fun `flags an inline read in a property initializer`() {
        val findings = lintProduction(
            """
            class Poller {
                private val startedAt = Clock.System.now()
            }
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `flags a fully qualified inline read`() {
        val findings = lintProduction("val now = kotlin.time.Clock.System.now()")

        assertEquals(1, findings.size)
    }

    @Test
    fun `flags the import alias evasion`() {
        // `System.now()` at the call site never spells Clock.System, so the import is the
        // only place a ban can see it.
        val findings = lintProduction(
            """
            import kotlin.time.Clock.System

            val now = System.now().epochSeconds
            """.trimIndent(),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `flags every inline read in a file, not just the first`() {
        val findings = lintProduction(
            """
            fun a() = Clock.System.now()
            fun b() = Clock.System.now()
            fun c() = Clock.System.now()
            """.trimIndent(),
        )

        assertEquals(3, findings.size)
    }

    @Test
    fun `does not flag a function parameter default value`() {
        val findings = lintProduction(
            """
            fun label(now: Instant = Clock.System.now()): String = now.toString()
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a constructor parameter default value`() {
        val findings = lintProduction(
            """
            class JourneyCard(private val clock: Clock = Clock.System)
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag an expression built around a parameter default value`() {
        val findings = lintProduction(
            """
            fun soon(at: Instant = Clock.System.now().plus(5.minutes)): Instant = at
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a Koin binding in a di package`() {
        val findings = ClockSystemBan(TestConfig()).lint(
            compileContentForTest(
                """
                package xyz.ksharma.krail.core.datetime.di

                import org.koin.dsl.module

                val dateTimeModule = module {
                    single<Clock> { Clock.System }
                }
                """.trimIndent(),
                filename = "/krail/core/date-time/src/commonMain/kotlin/DateTimeModule.kt",
            ),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `flags a di package file that is not a Koin module`() {
        // The exemption is for the one binding that puts the real clock in the graph, not for
        // everything that happens to sit in a di directory.
        val findings = ClockSystemBan(TestConfig()).lint(
            compileContentForTest(
                """
                package xyz.ksharma.krail.core.datetime.di

                val startedAt = Clock.System.now()
                """.trimIndent(),
                filename = "/krail/core/date-time/src/commonMain/kotlin/Helper.kt",
            ),
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag a test source`() {
        val findings = ClockSystemBan(TestConfig()).lint(
            compileContentForTest(
                "fun fixture() = Clock.System.now()",
                filename = "/krail/feature/track/ui/src/commonTest/kotlin/TripPollerTest.kt",
            ),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag an androidHostTest source`() {
        val findings = ClockSystemBan(TestConfig()).lint(
            compileContentForTest(
                "fun fixture() = Clock.System.now()",
                filename = "/krail/composeApp/src/androidHostTest/kotlin/Guard.kt",
            ),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `exempts by source set directory, never by file name`() {
        // A production file called Test.kt is still production.
        val findings = lintProduction(
            "fun now() = Clock.System.now()",
            filename = "/krail/core/date-time/src/commonMain/kotlin/Test.kt",
        )

        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag an injected clock read`() {
        val findings = lintProduction(
            """
            class Poller(private val clock: Clock) {
                fun now() = clock.now()
            }
            """.trimIndent(),
        )

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag an unrelated System reference`() {
        val findings = lintProduction("fun out() = System.out")

        assertEquals(0, findings.size)
    }

    private fun lintProduction(
        code: String,
        filename: String = "/krail/feature/x/src/commonMain/kotlin/Prod.kt",
    ) = ClockSystemBan(TestConfig()).lint(compileContentForTest(code, filename = filename))
}
