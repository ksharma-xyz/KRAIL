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
                lastServices += LastService(
                    from = parts[1],
                    leaves = to12Hour(parts[2]),
                    to = parts[GROUP_TO],
                    arrives = to12Hour(parts[GROUP_ARRIVES]),
                )
            }
            else -> paragraphs += normaliseTimes(line.replace(LIST_MARKER, ""))
        }
    }
    return DisruptionDetail(paragraphs = paragraphs, lastServices = lastServices)
}

// "11:10PM", "11:10 pm" or "23:03": clock time with an optional am or pm.
private val CLOCK_TIME = Regex("""\b(\d{1,2}):(\d{2})(\s?(?:AM|PM|am|pm))?\b""")

// "4AM": an hour with no minutes.
private val HOUR_ONLY = Regex("""\b(\d{1,2})\s?(AM|PM|am|pm)\b""")

// Capture group positions in LAST_SERVICE and CLOCK_TIME.
private const val GROUP_TO = 3
private const val GROUP_ARRIVES = 4
private const val GROUP_SUFFIX = 3

private const val NOON = 12
private const val HOURS_IN_HALF_DAY = 12

/** "23:03" as the rest of the app writes it: "11:03 PM". Text that is not a 24 hour time is returned as is. */
internal fun to12Hour(time: String): String {
    val match = Regex("""^(\d{1,2}):(\d{2})$""").matchEntire(time.trim()) ?: return time
    val hour = match.groupValues[1].toInt()
    val minutes = match.groupValues[2]
    val clockHour = if (hour % HOURS_IN_HALF_DAY == 0) HOURS_IN_HALF_DAY else hour % HOURS_IN_HALF_DAY
    return "$clockHour:$minutes ${if (hour < NOON) "AM" else "PM"}"
}

/**
 * Every time in [text] written the way the rest of the app writes it: "11:10PM" becomes "11:10 PM", "4AM"
 * becomes "4 AM" and "23:03" becomes "11:03 PM". NSW mixes all three in one alert. Safe to run twice.
 */
internal fun normaliseTimes(text: String): String {
    val withMinutes = CLOCK_TIME.replace(text) { match ->
        val suffix = match.groupValues[GROUP_SUFFIX].trim().uppercase()
        if (suffix.isEmpty()) to12Hour(match.value) else "${match.groupValues[1]}:${match.groupValues[2]} $suffix"
    }
    return HOUR_ONLY.replace(withMinutes) { "${it.groupValues[1]} ${it.groupValues[2].uppercase()}" }
}

// Times as NSW writes them: "11:10PM", "4AM", "23:03".
private val TIME = Regex("""\b\d{1,2}(?::\d{2})?\s?(?:AM|PM|am|pm)\b|\b(?:[01]?\d|2[0-3]):[0-5]\d\b""")

// A capitalised name straight after the words NSW puts before a place: "between Bankstown and Sydenham",
// "arriving Sydenham at 23:24", "from Redfern". Spelled with both cases because inline regex flags are not
// portable to iOS.
private val PLACE = Regex(
    """\b(?:between|Between|and|And|or|Or|from|From|to|To|at|At|arriving|Arriving|of|Of)\b\s+(?:the\s+)?""" +
        """([A-Z][a-z]+(?:\s[A-Z][a-z]+)*)""",
)

// Capitalised words that follow those prepositions but are not places.
private val NOT_PLACES = setOf(
    "Platform", "Platforms", "Station", "Stations", "Metro", "Service", "Services",
    "Bus", "Buses", "Train", "Trains",
)

/**
 * The runs of [text] to set in bold: times and place names.
 *
 * NSW sends only plain text for the details, so this recognises the phrasing of the four live metro
 * alerts and nothing more. Text it does not recognise simply gets no bold; it is never dropped or
 * changed, so an alert in a format we have not seen still reads in full. Ranges are sorted and do not
 * overlap.
 */
internal fun factRanges(text: String): List<IntRange> {
    val times = TIME.findAll(text).map { it.range }
    val places = PLACE.findAll(text)
        .mapNotNull { match -> match.groups[1]?.takeIf { it.value.substringBefore(' ') !in NOT_PLACES }?.range }
    val merged = mutableListOf<IntRange>()
    (times + places).sortedBy { it.first }.forEach { range ->
        val last = merged.lastOrNull()
        if (last == null || range.first > last.last) merged += range
    }
    return merged
}
