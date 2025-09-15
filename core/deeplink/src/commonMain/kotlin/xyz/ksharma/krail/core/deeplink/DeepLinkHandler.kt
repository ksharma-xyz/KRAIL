package xyz.ksharma.krail.core.deeplink

import xyz.ksharma.krail.core.log.log

/**
 * Handles parsing of deep links for the Krail app.
 *
 * Supports both custom scheme and universal links:
 * - Custom scheme: krail://timetable/{fromStopId}_{toStopId}
 * - Universal link: https://krail.app/timetable/{fromStopId}_{toStopId}
 *
 * Example: https://krail.app/timetable/200060_200080
 */
object DeepLinkHandler {

    private const val CUSTOM_SCHEME = "krail"
    private const val HTTPS_SCHEME = "https"
    private const val UNIVERSAL_LINK_HOST = "krail.app"
    private const val HOST_TIMETABLE = "timetable"

    /**
     * Parses a deep link URL and returns the appropriate route data if valid.
     *
     * @param url The deep link URL to parse
     * @return Map with route data if the URL is a valid timetable deep link, null otherwise
     */
    fun parseDeepLink(url: String): Map<String, String>? {
        log("DeepLinkHandler: Starting to parse deep link: $url")

        return try {
            val uri = parseUri(url)
            log("DeepLinkHandler: Parsed URI - scheme: ${uri.scheme}, host: ${uri.host}, path: ${uri.path}")

            when {
                uri.scheme == CUSTOM_SCHEME && uri.host == HOST_TIMETABLE -> {
                    log("DeepLinkHandler: Valid timetable deep link detected (custom scheme)")
                    parseTimeTableDeepLink(uri.path)
                }
                uri.scheme == HTTPS_SCHEME && uri.host == UNIVERSAL_LINK_HOST && uri.path.startsWith("/$HOST_TIMETABLE/") -> {
                    log("DeepLinkHandler: Valid timetable deep link detected (universal link)")
                    parseTimeTableDeepLink(uri.path.removePrefix("/$HOST_TIMETABLE/"))
                }
                else -> {
                    log("DeepLinkHandler: Invalid deep link format - scheme: ${uri.scheme}, host: ${uri.host}")
                    null
                }
            }
        } catch (e: Exception) {
            log("DeepLinkHandler: Error parsing deep link: ${e.message}")
            null
        }
    }

    private fun parseTimeTableDeepLink(path: String?): Map<String, String>? {
        log("DeepLinkHandler: Parsing timetable path: $path")

        if (path.isNullOrEmpty()) {
            log("DeepLinkHandler: Path is null or empty")
            return null
        }

        // Remove leading slash and parse the format: fromStopId_toStopId
        val cleanPath = path.removePrefix("/")
        log("DeepLinkHandler: Clean path: $cleanPath")

        val parts = cleanPath.split("_")
        log("DeepLinkHandler: Split parts: $parts")

        if (parts.size != 2) {
            log("DeepLinkHandler: Invalid path format - expected 2 parts, got ${parts.size}")
            return null
        }

        val fromStopId = parts[0].trim()
        val toStopId = parts[1].trim()

        if (fromStopId.isEmpty() || toStopId.isEmpty()) {
            log("DeepLinkHandler: Empty stop IDs - fromStopId: '$fromStopId', toStopId: '$toStopId'")
            return null
        }

        val result = mapOf(
            "type" to "TimeTableRoute",
            "fromStopId" to fromStopId,
            "fromStopName" to "", // We'll let the app resolve the name
            "toStopId" to toStopId,
            "toStopName" to "" // We'll let the app resolve the name
        )

        log("DeepLinkHandler: Successfully parsed timetable deep link: $result")
        return result
    }

    /**
     * Creates a deep link URL for a timetable route.
     */
    fun createTimeTableDeepLink(fromStopId: String, toStopId: String): String {
        return "$CUSTOM_SCHEME://$HOST_TIMETABLE/${fromStopId}_${toStopId}"
    }

    /**
     * Creates a universal link URL for sharing (clickable in messaging apps).
     * Uses a custom parameter to help with app detection.
     */
    fun createShareableTimeTableLink(fromStopId: String, toStopId: String): String {
        return "$HTTPS_SCHEME://$UNIVERSAL_LINK_HOST/$HOST_TIMETABLE/${fromStopId}_${toStopId}?app=krail"
    }

    private fun parseUri(url: String): Uri {
        log("DeepLinkHandler: Parsing URI: $url")

        // Simple URI parser for the expected format
        val schemeIndex = url.indexOf("://")
        if (schemeIndex == -1) {
            log("DeepLinkHandler: No scheme separator found")
            throw IllegalArgumentException("Invalid URL format")
        }

        val scheme = url.substring(0, schemeIndex)
        val remaining = url.substring(schemeIndex + 3)

        val pathIndex = remaining.indexOf("/")
        val host = if (pathIndex == -1) remaining else remaining.substring(0, pathIndex)
        val path = if (pathIndex == -1) "" else remaining.substring(pathIndex)

        log("DeepLinkHandler: Parsed - scheme: '$scheme', host: '$host', path: '$path'")
        return Uri(scheme, host, path)
    }

    private data class Uri(
        val scheme: String,
        val host: String,
        val path: String
    )
}
