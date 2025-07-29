package xyz.ksharma.krail.discover.state

sealed interface DiscoverEvent {
    data object ButtonClicked: DiscoverEvent
}
