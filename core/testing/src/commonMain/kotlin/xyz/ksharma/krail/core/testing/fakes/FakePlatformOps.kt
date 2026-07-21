package xyz.ksharma.krail.core.testing.fakes

import xyz.ksharma.krail.platform.ops.PlatformOps

class FakePlatformOps : PlatformOps {
    var lastSharedText: String? = null
    var lastShareTitle: String? = null
    var lastOpenedUrl: String? = null
    var lastMapDirections: MapDirections? = null

    override fun sharePlainText(text: String, title: String) {
        lastSharedText = text
        lastShareTitle = title
    }

    override fun openUrl(url: String) {
        lastOpenedUrl = url
    }

    override fun openMapDirections(latitude: Double, longitude: Double, label: String) {
        lastMapDirections = MapDirections(latitude = latitude, longitude = longitude, label = label)
    }

    data class MapDirections(val latitude: Double, val longitude: Double, val label: String)
}
