package xyz.ksharma.krail.core.testing.device

/**
 * How much of each edge of a window belongs to the system, in dp on Android and pt on iOS (Compose treats the two alike).
 * Which window insets type carries each number is decided by the profile's [PlatformInsetShape], not here.
 */
data class SystemInsets(
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
) {
    override fun toString(): String = "L$left T$top R$right B$bottom"
}

/**
 * Which window insets type the platform puts each inset in. The same 84 dp side strip is a different thing to a screen that
 * pads by `displayCutout` on Android and on iOS, which is what hid Search Stop's controls under the iPhone Duo status pill
 * while every Android test passed (`docs/testing/DEVICE_PROFILES.md`).
 */
enum class PlatformInsetShape {
    /**
     * A phone with gesture or three button navigation along the bottom. The top is `statusBars`, the bottom is
     * `navigationBars` and a side inset is a camera cutout, so it arrives as `displayCutout`.
     */
    ANDROID,

    /** A phone in landscape with the navigation bar on its side. A side inset arrives as `navigationBars`. */
    ANDROID_SIDE_NAVIGATION_BAR,

    /**
     * What Compose Multiplatform reported on the outer screen of the iPhone Duo. The top is `statusBars`, the bottom is
     * `navigationBars`, and a side inset arrives only in `systemBars`, `safeDrawing`, `safeContent` and `safeGestures`, while
     * `displayCutout` is 0. On Android the one system bar type that only those readers read is the caption bar, so the side
     * strip is delivered as that: a screen padding by `displayCutout`, `statusBars` or `navigationBars` alone leaves it
     * unpadded, exactly as it does on the iPhone.
     */
    IOS,
}

/**
 * One device a screen is composed on: its window, density and font scale, the system insets, and where those numbers came from.
 *
 * [source] must start with `measured` (and say which probe, where and when) or `assumed` (and say why). It is checked on
 * construction so a number can never go in without saying which it is.
 */
data class DeviceProfile(
    val name: String,
    val widthDp: Int,
    val heightDp: Int,
    val density: Float,
    val fontScale: Float = 1f,
    val insets: SystemInsets = SystemInsets(),
    val shape: PlatformInsetShape,
    val source: String,
) {
    init {
        require(widthDp > 0 && heightDp > 0) { "$name has no window: ${widthDp}x$heightDp" }
        require(density > 0f && fontScale > 0f) { "$name has density $density and font scale $fontScale" }
        require(source.startsWith(MEASURED) || source.startsWith(ASSUMED)) {
            "$name: source must start with '$MEASURED' or '$ASSUMED', and say which probe or why. Got '$source'"
        }
    }

    /** The profile's numbers as one line for a failure message, with where they came from. */
    fun describe(): String =
        "$name ${widthDp}x$heightDp density $density font $fontScale insets $insets ($shape, $source)"

    override fun toString(): String = name

    companion object {
        const val MEASURED = "measured"
        const val ASSUMED = "assumed"
    }
}
