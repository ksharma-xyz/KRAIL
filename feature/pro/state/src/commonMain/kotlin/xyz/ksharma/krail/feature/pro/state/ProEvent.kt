package xyz.ksharma.krail.feature.pro.state

sealed interface ProEvent {
    data class SelectPlan(val plan: ProPlan) : ProEvent
    data object SubscribeTapped : ProEvent
    data object RestorePurchaseTapped : ProEvent
}
