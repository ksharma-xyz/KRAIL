package xyz.ksharma.krail.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType

/**
 * `isSystemInDarkTheme()` is the phone's dark setting. The rider picks light, dark or system in
 * the app (`ThemeMode`), and every screen has to follow that choice, so anything that decides
 * light or dark asks taj's `isAppInDarkMode()` instead. See `taj/THEMING.md`.
 *
 * The phone's value is only read in one place: taj's theme package, where `isAppInDarkMode()`
 * turns `ThemeMode.SYSTEM` into a boolean and applies the iOS stale-value override
 * (`LocalSystemDarkThemeOverride`). Everywhere else it is a bug that shows up as soon as the phone
 * and the app disagree: a phone in light mode and an app set to Dark gets dark status-bar icons on
 * a dark screen.
 *
 * Pre-existing reads are grandfathered in `config/system-dark-theme-baseline.txt`, one line per
 * file with how many reads it may still contain. The file only shrinks: fixing a read means
 * decrementing its count in the same change.
 *
 * No type resolution, so the check is syntactic: a call whose callee is named
 * `isSystemInDarkTheme`, qualified or not. Commented-out code is not code and is not flagged.
 */
class SystemDarkThemeBan(config: Config) : Rule(config) {

    override val issue = Issue(
        id = "SystemDarkThemeBan",
        severity = Severity.Defect,
        description = MESSAGE,
        debt = Debt.TEN_MINS,
    )

    private val baseline = RatchetBaseline(BASELINE_FILE)

    override fun visitKtFile(file: KtFile) {
        super.visitKtFile(file)

        if (file.packageFqName.asString() == THEME_PACKAGE) return
        if (TEST_SOURCE_SET_MARKERS.any { file.name.replace('\\', '/').contains(it) }) return

        val reads = file.collectDescendantsOfType<KtCallExpression> { it.isSystemDarkRead() }
            .sortedBy { it.textOffset }
        if (reads.isEmpty()) return

        reads.drop(allowanceFor(file, reads.size)).forEach { call ->
            report(CodeSmell(issue, Entity.from(call), MESSAGE))
        }
    }

    private fun KtCallExpression.isSystemDarkRead(): Boolean =
        calleeExpression?.text == SYSTEM_DARK_CALL

    /**
     * The largest listed count for this file that is not more than [found]. The entry is exact
     * text, so each candidate count is asked for in turn.
     */
    private fun allowanceFor(file: KtFile, found: Int): Int {
        val path = baseline.relativePath(file) ?: return 0
        return (found downTo 1).firstOrNull { baseline.contains(file, "$path|$it") } ?: 0
    }

    private companion object {
        const val SYSTEM_DARK_CALL = "isSystemInDarkTheme"
        const val THEME_PACKAGE = "xyz.ksharma.krail.taj.theme"
        const val BASELINE_FILE = "system-dark-theme-baseline.txt"
        val TEST_SOURCE_SET_MARKERS = listOf("/src/commonTest/", "/src/androidHostTest/", "/src/iosTest/", "/src/test/")

        const val MESSAGE = "isSystemInDarkTheme() is the phone's setting, not the rider's in-app " +
            "ThemeMode. Use isAppInDarkMode() from taj, which follows ThemeMode and applies the " +
            "iOS override. See taj/THEMING.md."
    }
}
