package xyz.ksharma.krail.platform.ops

interface PlatformOps {

    /**
     * Will share plain text with other system apps.
     *
     * @param text - the text to share (can contain emoji and other characters)
     */
    fun sharePlainText(text: String, title: String = "")

    /**
     * Will open the given URL in the system browser.
     *
     * @param url - the URL to open
     */
    fun openUrl(url: String)

    /**
     * Opens turn-by-turn directions to a coordinate in whichever maps app the device treats as
     * default.
     *
     * Deliberately not a plain [openUrl] with a hardcoded provider URL: that would send every
     * rider to Google Maps in a browser regardless of what they actually use. Each platform
     * builds the URI its own launcher understands, so the system picks the app.
     *
     * @param label human-readable destination name, shown as the pin title where supported.
     */
    fun openMapDirections(latitude: Double, longitude: Double, label: String)
}
