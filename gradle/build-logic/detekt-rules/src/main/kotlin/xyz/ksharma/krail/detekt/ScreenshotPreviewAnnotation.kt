package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtNamedFunction

/**
 * A `@ScreenshotTest` preview must be declared with `@PreviewComponent` or `@PreviewScreen`,
 * never with a bare `@Preview`.
 *
 * `@Preview` renders one configuration. `@PreviewComponent` and `@PreviewScreen` (in
 * `xyz.ksharma.krail.taj.preview`) expand to light mode, dark mode, 2x font scale — and, for
 * `@PreviewScreen`, a tablet. A screenshot test built on a bare `@Preview` therefore records a
 * single light-mode phone baseline and silently covers none of the configurations that actually
 * break: a dark-mode-only contrast regression or a 2x-font-scale clipping bug lands green.
 *
 * That is why CLAUDE.md's "Preview Annotations" section calls this out explicitly for the
 * screenshot-test case, not just for IDE previews.
 *
 * Pre-existing offenders are grandfathered in [BASELINE_FILE] as `<repo-relative path>|<function
 * name>`. Fix one by swapping its `@Preview` for `@PreviewComponent`/`@PreviewScreen`, re-recording
 * the baseline images, and deleting its line from that file in the same change.
 */
class ScreenshotPreviewAnnotation(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "ScreenshotPreviewAnnotation",
        severity = Severity.Defect,
        description = "@ScreenshotTest must be paired with @PreviewComponent or @PreviewScreen " +
            "so the recorded baseline covers dark mode and 2x font scale, not just one " +
            "light-mode phone.",
        debt = Debt.TEN_MINS,
    )

    private val baseline = RatchetBaseline(BASELINE_FILE)

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val annotationNames = function.annotationEntries
            .mapNotNull { it.shortName?.asString() }
            .toSet()
        if (SCREENSHOT_TEST_ANNOTATION !in annotationNames) return
        if (annotationNames.any { it in MULTI_PREVIEW_ANNOTATIONS }) return

        val name = function.name ?: return
        val path = baseline.relativePath(function.containingKtFile)
        if (path != null && baseline.contains(function.containingKtFile, "$path|$name")) return

        report(
            CodeSmell(
                issue = issue,
                entity = Entity.atName(function),
                message = "'$name' is a @ScreenshotTest but is not annotated " +
                    "@PreviewComponent or @PreviewScreen, so its baseline only covers one " +
                    "configuration. Replace @Preview with @PreviewComponent (components) or " +
                    "@PreviewScreen (full screens) — see CLAUDE.md, 'Preview Annotations'.",
            ),
        )
    }

    companion object {
        const val BASELINE_FILE = "screenshot-annotation-baseline.txt"

        private const val SCREENSHOT_TEST_ANNOTATION = "ScreenshotTest"

        private val MULTI_PREVIEW_ANNOTATIONS = setOf("PreviewComponent", "PreviewScreen")
    }
}
