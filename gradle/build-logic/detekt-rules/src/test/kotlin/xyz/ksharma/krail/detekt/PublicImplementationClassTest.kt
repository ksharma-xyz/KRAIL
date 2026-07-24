package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.test.TestConfig
import io.gitlab.arturbosch.detekt.test.compileAndLint
import kotlin.test.Test
import kotlin.test.assertEquals

class PublicImplementationClassTest {

    @Test
    fun `flags a public class implementing an interface`() {
        val code = """
            interface Greeter {
                fun greet(): String
            }

            class RealGreeter : Greeter {
                override fun greet(): String = "hi"
            }
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `flags a public class even when its interface is declared elsewhere`() {
        // No local interface declaration at all here — this is the realistic shape in this
        // codebase: impl and interface live in different files (or different modules), so the
        // rule can't and doesn't try to verify the supertype resolves to a local interface.
        val code = """
            class RealGreeter : Greeter {
                override fun greet(): String = "hi"
            }
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag an internal implementation`() {
        val code = """
            internal class RealGreeter : Greeter {
                override fun greet(): String = "hi"
            }
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a class with no supertype`() {
        val code = """
            class PlainValue(val value: Int)
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a data class`() {
        val code = """
            data class RealGreeter(val name: String) : Greeter
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a sealed class`() {
        val code = """
            sealed class Result : Greeter
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a class implementing a default-excluded super type`() {
        val code = """
            class MyException : Throwable()
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `respects the excludeSuperTypes config`() {
        val code = """
            class RealGreeter : Greeter {
                override fun greet(): String = "hi"
            }
        """.trimIndent()

        val config = TestConfig(PublicImplementationClass.EXCLUDE_SUPER_TYPES_KEY to listOf("Greeter"))
        val findings = PublicImplementationClass(config).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `does not flag a class extending an abstract class via constructor call`() {
        // `: ViewModel()` is a superclass constructor call, never interface implementation —
        // the parens are the tell, and need no type resolution to read.
        val code = """
            class MyViewModel : ViewModel()
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }

    @Test
    fun `flags only the interface half of a mixed supertype list`() {
        val code = """
            class RealGreeter : BaseClass(), Greeter {
                override fun greet(): String = "hi"
            }
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(1, findings.size)
        assertEquals(true, findings.single().message.contains("Greeter"))
        assertEquals(false, findings.single().message.contains("BaseClass"))
    }

    @Test
    fun `flags delegated interface implementation`() {
        val code = """
            class RealGreeter(delegate: Greeter) : Greeter by delegate
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(1, findings.size)
    }

    @Test
    fun `does not flag a nested class`() {
        val code = """
            class Outer {
                class Inner : Greeter
            }
        """.trimIndent()

        val findings = PublicImplementationClass(TestConfig()).compileAndLint(code)

        assertEquals(0, findings.size)
    }
}
