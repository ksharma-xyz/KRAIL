package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry

/**
 * Flags a top-level `class` that is public (the Kotlin default), implements at least one
 * supertype, and isn't otherwise excluded — the shape of a DI implementation class that
 * should be `internal` instead.
 *
 * The intended pattern in this codebase: interfaces are the public surface a module exposes;
 * their implementations are wired to the interface via Koin (`single<Interface> { Impl() }`)
 * within the same module and are never meant to be depended on by concrete type from outside
 * it, so they should be `internal` (or `private`).
 *
 * ## Why this is a name-based heuristic, not a resolved one
 *
 * Detekt's Gradle plugin runs one task per module and only sees that module's own files, so a
 * rule can never confirm "this class is referenced only within its own module" — the compiler
 * already does that job better than a lint ever could: once a flagged class is made `internal`,
 * any other module that was (incorrectly) depending on it by concrete type simply fails to
 * compile.
 *
 * Ideally this rule would also confirm the implemented supertype is an `interface` declared in
 * this codebase (as opposed to a binary/library contract like `Parcelable`) using detekt's type
 * resolution (`bindingContext`). In practice, detekt's Kotlin Multiplatform support does not
 * populate a usable binding context for this project's module structure — verified directly:
 * the equivalent `@RequiresTypeResolution` implementation produced zero findings against a
 * deliberately broken sample class, on both the experimental `detektAndroidMain` task and the
 * `detektMetadataCommonMain` task, while the same rule logic correctly flagged the identical
 * code in a unit test harness with type resolution set up explicitly. So instead: match by raw
 * supertype name.
 *
 * One purely syntactic check needs no resolution at all and is applied unconditionally: a
 * supertype written as `Foo(args)` is always a superclass constructor call (extending an
 * abstract/open class, e.g. `ViewModel()`), never interface implementation, so it's excluded
 * before the name ever reaches [EXCLUDE_SUPER_TYPES_KEY]. What's left after that — plain `Foo`
 * or delegated `Foo by x` — is genuinely ambiguous without resolution (could be this codebase's
 * own interface, or an external one like `Parcelable`/`KoinComponent`/Compose's `Shape`), which
 * is what [EXCLUDE_SUPER_TYPES_KEY] and [DEFAULT_EXCLUDED_SUPER_TYPES] are for.
 */
class PublicImplementationClass(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "PublicImplementationClass",
        severity = Severity.Style,
        description = "Implementation classes bound to an interface via DI should be " +
            "'internal' (or 'private'); only the interface is meant to be the public surface.",
        debt = Debt.FIVE_MINS,
    )

    private val excludeSuperTypes: List<String> by lazy {
        DEFAULT_EXCLUDED_SUPER_TYPES + valueOrDefault(EXCLUDE_SUPER_TYPES_KEY, emptyList())
    }

    override fun visitClass(klass: KtClass) {
        super.visitClass(klass)
        if (!isCandidate(klass)) return

        val superTypeNames = rawSuperTypeSimpleNames(klass).filterNot { it in excludeSuperTypes }
        if (superTypeNames.isEmpty()) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.atName(klass),
                message = "'${klass.name}' implements ${superTypeNames.joinToString()} but is " +
                    "declared public. Make it 'internal' (or 'private') and depend on the " +
                    "interface everywhere else, or add it to '$EXCLUDE_SUPER_TYPES_KEY' in " +
                    "detekt.yml if it genuinely needs to stay public.",
            ),
        )
    }

    /** Top-level, concrete (non-abstract/sealed), not already narrowed, has a supertype list. */
    private fun isCandidate(klass: KtClass): Boolean {
        if (!klass.isTopLevel()) return false
        if (klass.isInterface()) return false
        if (klass.isEnum()) return false
        if (klass.isAnnotation()) return false
        if (klass.isData()) return false
        if (klass.isSealed()) return false
        if (klass.hasModifier(KtTokens.ABSTRACT_KEYWORD)) return false
        if (klass.hasModifier(KtTokens.PRIVATE_KEYWORD)) return false
        if (klass.hasModifier(KtTokens.INTERNAL_KEYWORD)) return false
        if (klass.hasModifier(KtTokens.PROTECTED_KEYWORD)) return false
        return klass.getSuperTypeList() != null
    }

    /**
     * Simple (unqualified, un-generic'd) names of the supertypes that look like interface
     * implementation, i.e. `: Foo` or `: Foo by delegate` — not `: Foo(args)`, which is always a
     * superclass constructor call (extending an abstract/open class, e.g. `ViewModel()`,
     * `AndroidSqliteDriver.Callback(schema)`), never interface implementation. This syntactic
     * split needs no type resolution and reliably tells the two apart.
     */
    private fun rawSuperTypeSimpleNames(klass: KtClass): List<String> =
        klass.getSuperTypeList()?.entries.orEmpty()
            .filterNot { it is KtSuperTypeCallEntry }
            .mapNotNull { entry -> entry.typeAsUserType?.referencedName }

    companion object {
        const val EXCLUDE_SUPER_TYPES_KEY = "excludeSuperTypes"

        /**
         * Common external/library contracts a class can implement while staying legitimately
         * public — none of these are "this codebase's own module interface" the rule targets.
         * Extend per-module via [EXCLUDE_SUPER_TYPES_KEY] in detekt.yml rather than editing this.
         */
        val DEFAULT_EXCLUDED_SUPER_TYPES = listOf(
            "Parcelable",
            "Serializable",
            "Comparable",
            "Comparator",
            "Iterable",
            "Iterator",
            "Closeable",
            "AutoCloseable",
            "Runnable",
            "CharSequence",
            "Throwable",
            "Exception",
            "RuntimeException",
            "IllegalStateException",
            "IllegalArgumentException",
            "CoroutineScope",
            "NavKey",
            "KoinComponent",
            "Shape",
            "Node",
            "DrawModifierNode",
            "LayoutModifierNode",
            "PointerInputModifierNode",
            "SemanticsModifierNode",
        )
    }
}
