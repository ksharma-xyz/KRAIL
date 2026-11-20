package xyz.ksharma.krail.trip.planner.ui.timetable.business

/** "Bankstown at 23:03, arriving Sydenham at 23:24" as parts. */
internal data class LastService(val from: String, val leaves: String, val to: String, val arrives: String)

internal data class DisruptionDetail(val paragraphs: List<String>, val lastServices: List<LastService>)

// "Monday 23 November": the headline and the first line already say when, so this only repeats it.
private val DATE_LINE = Regex("^(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day \\d{1,2} \\p{L}+$")
private val LAST_SERVICE = Regex("^[-*]\\s*(.+?) at (\\d{1,2}:\\d{2}),?\\s+arriving (.+?) at (\\d{1,2}:\\d{2})\\.?$")
private val LIST_MARKER = Regex("^[-*]\\s*")

/**
 * Splits an alert description into sentences and a last-services list.
 *
 * NSW writes these as plain text with fixed phrases, checked against the four live metro alerts:
 * a date line, "From 11:10PM, buses replace ...", "Services run between ...", "The last metro
 * services leave:" followed by "- X at 23:03, arriving Y at 23:24" lines, and advice such as "Use
 * trains between ...". Anything that matches no phrase is kept as a paragraph, so an unfamiliar
 * alert still shows all of its text.
 */
internal fun parseDisruptionDetail(detail: String): DisruptionDetail {
    val paragraphs = mutableListOf<String>()
    val lastServices = mutableListOf<LastService>()
    detail.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
        val lastService = LAST_SERVICE.matchEntire(line)
        when {
            DATE_LINE.matches(line) -> Unit
            // The heading of the list below, which the text supplies itself.
            line.endsWith(":") && "last" in line.lowercase() -> Unit
            lastService != null -> {
                val parts = lastService.groupValues
                lastServices += LastService(from = parts[1], leaves = parts[2], to = parts[3], arrives = parts[4])
            }
            else -> paragraphs += line.replace(LIST_MARKER, "")
        }
    }
    return DisruptionDetail(paragraphs = paragraphs, lastServices = lastServices)
}

/**
 * The description as plain text for the tile: one paragraph per sentence with a blank line between,
 * and the last services as short lines under a heading, without NSW's "- " list markers.
 */
internal fun formatDisruptionDescription(detail: String): String {
    val parsed = parseDisruptionDetail(detail)
    val blocks = parsed.paragraphs.toMutableList()
    if (parsed.lastServices.isNotEmpty()) {
        val lines = parsed.lastServices.map { "${it.from} ${it.leaves}, arrives ${it.to} ${it.arrives}" }
        blocks += (listOf("Last services") + lines).joinToString("\n")
    }
    return blocks.joinToString("\n\n")
}
