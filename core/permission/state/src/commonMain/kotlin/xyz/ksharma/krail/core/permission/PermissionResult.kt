package xyz.ksharma.krail.core.permission

sealed class PermissionResult {
    data object Granted : PermissionResult()
    data class Denied(val isPermanent: Boolean) : PermissionResult()
    data object Cancelled : PermissionResult()
}
