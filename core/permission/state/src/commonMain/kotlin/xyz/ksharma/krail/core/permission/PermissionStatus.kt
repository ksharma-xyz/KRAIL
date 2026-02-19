package xyz.ksharma.krail.core.permission

sealed class PermissionStatus {
    data object NotDetermined : PermissionStatus()
    data object Granted : PermissionStatus()
    data object Denied : PermissionStatus()
}
