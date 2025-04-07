package xyz.ksharma.krail.platform.ops

interface ContentSharing {

    /**
     * Will share plain text with other system apps.
     *
     * @param text - the text to share (can contain emoji and other characters)
     */
    fun sharePlainText(text: String)
}
