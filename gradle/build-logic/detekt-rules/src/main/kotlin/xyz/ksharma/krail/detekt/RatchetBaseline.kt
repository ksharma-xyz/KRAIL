package xyz.ksharma.krail.detekt

import java.io.File
import org.jetbrains.kotlin.psi.KtFile

/**
 * A shrinking allow-list of pre-existing violations, read from a plain text file under
 * `config/` at the repository root.
 *
 * This is the same ratchet shape as `config/test-wiring-baseline.txt`: the rule fails the build
 * for any NEW violation, while the entries listed here are grandfathered. Fixing an offender
 * means DELETING its line in the same change, so the file only ever shrinks. When it reaches
 * zero entries, delete the file and the rule becomes an unconditional gate.
 *
 * ## Why not detekt's own `baseline.xml`?
 *
 * Detekt's baseline is per-module (`$projectDir/baseline.xml`, see `Detekt.kt`) and is regenerated
 * wholesale by `detektBaseline`, so it is trivially widened by accident and carries no explanation
 * of what is grandfathered or why. A hand-maintained text file at the repo root keeps every
 * offender for a given rule visible in one place, in a diff a reviewer can read.
 *
 * ## Locating the file
 *
 * Detekt runs one task per Gradle module and a rule is given no project metadata, so the repo root
 * is recovered from the analysed file's own path: walk up parent directories until one contains
 * `config/<fileName>`. Entries are keyed by a path relative to that root, so they are stable
 * regardless of which module's detekt task is running.
 *
 * Lines that are blank or start with `#` are comments and ignored.
 */
internal class RatchetBaseline(private val fileName: String) {

    private val cache = HashMap<String, Set<String>>()

    /** Repo-root-relative, `/`-separated path of [ktFile], or null if the root can't be found. */
    fun relativePath(ktFile: KtFile): String? {
        val absolute = File(ktFile.virtualFilePath).absoluteFile
        val root = repoRootOf(absolute) ?: return null
        return absolute.relativeToOrNull(root)?.invariantSeparatorsPath
    }

    /** True when [entry] is grandfathered for the repo owning [ktFile]. */
    fun contains(ktFile: KtFile, entry: String): Boolean {
        val root = repoRootOf(File(ktFile.virtualFilePath).absoluteFile) ?: return false
        return entriesOf(root).contains(entry)
    }

    private fun repoRootOf(file: File): File? {
        var dir: File? = file.parentFile
        while (dir != null) {
            if (File(File(dir, CONFIG_DIR), fileName).isFile) return dir
            dir = dir.parentFile
        }
        return null
    }

    @Synchronized
    private fun entriesOf(root: File): Set<String> = cache.getOrPut(root.path) {
        File(File(root, CONFIG_DIR), fileName)
            .readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toSet()
    }

    private fun File.relativeToOrNull(base: File): File? =
        runCatching { relativeTo(base) }.getOrNull()

    private companion object {
        const val CONFIG_DIR = "config"
    }
}
