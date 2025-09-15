package xyz.ksharma.krail.core.deeplink

import xyz.ksharma.krail.core.log.log

/**
 * Deep link validator that follows security best practices.
 * Validates and sanitizes both krail:// and https://krail.app deep links before processing.
 *
 * ✅ Valid URLs:
 * - krail://timetable/200060_200080
 * - https://krail.app/timetable/200060_200080
 * - krail://timetable/G1232_T456
 *
 * ❌ Invalid URLs (blocked):
 * - krail://timetable/123_123 (same origin/destination)
 * - javascript://alert('xss') (malicious scheme)
 * - krail://timetable/ (missing parameters)
 * - https://malicious.com/timetable/123_456 (wrong domain)
 */
object DeepLinkValidator {

    // Allowed schemes
    private val ALLOWED_SCHEMES = setOf("krail", "https")

    // Allowed hosts for different schemes
    private val ALLOWED_KRAIL_HOSTS = setOf("timetable")
    private val ALLOWED_HTTPS_HOSTS = setOf("krail.app")

    // Maximum URL length to prevent DoS attacks
    private const val MAX_URL_LENGTH = 200 // Reduced since we only support simple URLs

    // Maximum parameter value length
    private const val MAX_PARAM_LENGTH = 50

    // Allow alphanumeric stop IDs up to 10 characters
    private val STOP_ID_PATTERN = Regex("^[a-zA-Z0-9]{1,10}$")

    /**
     * Validates a krail:// deep link URL according to security best practices.
     *
     * @param url The URL to validate
     * @return ValidationResult with success/failure and details
     */
    fun validateDeepLink(url: String): ValidationResult {
        log("DeepLinkValidator: Starting validation for URL: $url")

        return try {
            // 1. Basic URL validation
            val basicValidation = validateBasicUrl(url)
            if (!basicValidation.isValid) {
                return basicValidation
            }

            // 2. Parse and validate URI structure
            val uri = parseAndValidateUri(url) ?: return ValidationResult.Invalid("Invalid URI format")

            // 3. Validate scheme (only krail://)
            if (uri.scheme !in ALLOWED_SCHEMES) {
                log("DeepLinkValidator: Invalid scheme: ${uri.scheme}")
                return ValidationResult.Invalid("Only krail:// and https://krail.app schemes are supported")
            }

            // 4. Validate host
            val isKrailScheme = uri.scheme == "krail"
            val allowedHosts = if (isKrailScheme) ALLOWED_KRAIL_HOSTS else ALLOWED_HTTPS_HOSTS

            if (uri.host !in allowedHosts) {
                log("DeepLinkValidator: Invalid host: ${uri.host}")
                return ValidationResult.Invalid("Unknown deep link type: ${uri.host}")
            }

            // 5. Validate timetable deep link
            validateTimeTableDeepLink(uri.path)

        } catch (e: Exception) {
            log("DeepLinkValidator: Validation failed with exception: ${e.message}")
            ValidationResult.Invalid("Validation error: ${e.message}")
        }
    }

    /**
     * Validates basic URL properties
     */
    private fun validateBasicUrl(url: String): ValidationResult {
        // Check URL length
        if (url.length > MAX_URL_LENGTH) {
            log("DeepLinkValidator: URL too long: ${url.length} characters")
            return ValidationResult.Invalid("URL exceeds maximum length")
        }

        // Check for null bytes and other suspicious characters
        if (url.contains('\u0000') || url.contains('\n') || url.contains('\r')) {
            log("DeepLinkValidator: URL contains suspicious characters")
            return ValidationResult.Invalid("URL contains invalid characters")
        }

        // Basic URL format check
        if (!url.contains("://")) {
            log("DeepLinkValidator: URL missing scheme separator")
            return ValidationResult.Invalid("Invalid URL format")
        }

        return ValidationResult.Valid
    }

    /**
     * Parses and validates URI structure
     */
    private fun parseAndValidateUri(url: String): Uri? {
        return try {
            val schemeIndex = url.indexOf("://")
            if (schemeIndex == -1) return null

            val scheme = url.substring(0, schemeIndex).lowercase()
            val remaining = url.substring(schemeIndex + 3)

            val pathIndex = remaining.indexOf("/")
            val host = if (pathIndex == -1) remaining else remaining.substring(0, pathIndex)
            val path = if (pathIndex == -1) "" else remaining.substring(pathIndex)

            // Validate components
            if (scheme.isEmpty() || host.isEmpty()) return null

            Uri(scheme, host, path)
        } catch (e: Exception) {
            log("DeepLinkValidator: URI parsing failed: ${e.message}")
            null
        }
    }

    /**
     * Validates timetable deep link format and parameters
     */
    private fun validateTimeTableDeepLink(path: String): ValidationResult {
        log("DeepLinkValidator: Validating timetable deep link path: $path")

        if (path.isEmpty() || path == "/") {
            return ValidationResult.Invalid("Missing timetable parameters")
        }

        // Remove leading slash and parse
        val cleanPath = path.removePrefix("/")

        // Check path length
        if (cleanPath.length > MAX_PARAM_LENGTH) {
            return ValidationResult.Invalid("Timetable path too long")
        }

        // Validate format: fromStopId_toStopId
        val parts = cleanPath.split("_")
        if (parts.size != 2) {
            log("DeepLinkValidator: Invalid timetable format, expected 2 parts, got ${parts.size}")
            return ValidationResult.Invalid("Invalid timetable format")
        }

        val fromStopId = parts[0].trim()
        val toStopId = parts[1].trim()

        // Validate stop IDs
        val fromValidation = validateStopId(fromStopId, "fromStopId")
        if (!fromValidation.isValid) return fromValidation

        val toValidation = validateStopId(toStopId, "toStopId")
        if (!toValidation.isValid) return toValidation

        // Check if from and to are different
        if (fromStopId == toStopId) {
            log("DeepLinkValidator: From and to stop IDs are the same")
            return ValidationResult.Invalid("Origin and destination cannot be the same")
        }

        log("DeepLinkValidator: Timetable deep link validated successfully")
        return ValidationResult.Valid
    }

    /**
     * Validates individual stop ID
     */
    private fun validateStopId(stopId: String, paramName: String): ValidationResult {
        if (stopId.isEmpty()) {
            return ValidationResult.Invalid("$paramName is empty")
        }

        if (stopId.length > 10) {
            return ValidationResult.Invalid("$paramName too long")
        }

        if (!STOP_ID_PATTERN.matches(stopId)) {
            log("DeepLinkValidator: Invalid stop ID format: $stopId")
            return ValidationResult.Invalid("$paramName contains invalid characters")
        }

        return ValidationResult.Valid
    }

    /**
     * Sanitizes a validated URL by removing potentially harmful characters
     */
    fun sanitizeUrl(url: String): String {
        return url.trim()
            .replace(Regex("\\s+"), "") // Remove all whitespace
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // Remove control characters
    }

    private data class Uri(
        val scheme: String,
        val host: String,
        val path: String
    )
}

/**
 * Result of deep link validation
 */
sealed class ValidationResult {
    abstract val isValid: Boolean

    object Valid : ValidationResult() {
        override val isValid = true
    }

    data class Invalid(val reason: String) : ValidationResult() {
        override val isValid = false
    }
}
