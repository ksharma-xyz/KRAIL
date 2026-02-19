package xyz.ksharma.krail.core.permission

sealed class PermissionResult {
    data object Granted : PermissionResult()
    data object Denied : PermissionResult()
    data object Cancelled : PermissionResult()
}
