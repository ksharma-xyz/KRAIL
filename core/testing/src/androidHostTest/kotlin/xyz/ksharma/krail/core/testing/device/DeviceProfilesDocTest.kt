package xyz.ksharma.krail.core.testing.device

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * The profile table in `docs/testing/DEVICE_PROFILES.md` is the spec and `DeviceProfiles` is the code, and the two must name
 * the same devices. A profile added in code and not written up, or written up and never added, fails here with the names, so the
 * page cannot drift from what the tests run on.
 *
 * Both of the page's tables (geometry and insets) are read: a row is a line starting with the profile's name in backticks.
 */
class DeviceProfilesDocTest {

    @Test
    fun `the geometry table and the inset table name every profile in code and nothing else`() {
        val doc = File(repoRoot(), DOC_PATH)
        if (!doc.isFile) fail("$DOC_PATH is missing: it is the spec for DeviceProfiles")
        val lines = doc.readLines()
        val inCode = DeviceProfiles.all.map { it.name }.toSet()

        val geometry = lines.rowNamesBetween("### Geometry", "### Insets the platform reports")
        val insets = lines.rowNamesBetween("### Insets the platform reports", "### How the shape delivers")

        assertEquals(inCode, geometry.toSet(), "the geometry table in $DOC_PATH and DeviceProfiles.all differ")
        assertEquals(inCode, insets.toSet(), "the inset table in $DOC_PATH and DeviceProfiles.all differ")
        assertEquals(inCode.size, geometry.size, "the geometry table names a profile twice")
        assertEquals(inCode.size, insets.size, "the inset table names a profile twice")
    }

    private fun List<String>.rowNamesBetween(start: String, end: String): List<String> {
        val from = indexOfFirst { it.startsWith(start) }
        val to = indexOfFirst { it.startsWith(end) }
        if (from < 0 || to < 0 || to <= from) fail("$DOC_PATH no longer has the sections '$start' and '$end'")
        return subList(from, to).mapNotNull { ROW_NAME.find(it)?.groupValues?.get(1) }
    }

    /** Anchored on `settings.gradle.kts` so moving the module does not silently make this read nothing. */
    private fun repoRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, "settings.gradle.kts").isFile) return candidate
            candidate = candidate.parentFile
        }
        fail("Could not find the repo root (no settings.gradle.kts above ${File(".").absolutePath})")
    }

    private companion object {
        const val DOC_PATH = "docs/testing/DEVICE_PROFILES.md"
        val ROW_NAME = Regex("""^\| `(\w+)` """)
    }
}
