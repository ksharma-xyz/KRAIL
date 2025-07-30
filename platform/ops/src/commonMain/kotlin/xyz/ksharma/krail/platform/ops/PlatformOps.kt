package xyz.ksharma.krail.platform.ops

interface PlatformOps {

    /**
     * Will share plain text with other system apps.
     *
     * @param text - the text to share (can contain emoji and other characters)
     */
    fun sharePlainText(text: String, title: String)

    /**
     * Will open the given URL in the system browser.
     *
     * @param url - the URL to open
     */
    fun openUrl(url: String)
}
