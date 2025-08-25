package xyz.ksharma.core.test.fakes

import xyz.ksharma.krail.platform.ops.PlatformOps

class FakePlatformOps : PlatformOps {
    var lastSharedText: String? = null
    var lastShareTitle: String? = null
    var lastOpenedUrl: String? = null

    override fun sharePlainText(text: String, title: String) {
        lastSharedText = text
        lastShareTitle = title
    }

    override fun openUrl(url: String) {
        lastOpenedUrl = url
    }
}