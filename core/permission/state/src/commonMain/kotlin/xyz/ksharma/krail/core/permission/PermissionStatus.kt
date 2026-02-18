package xyz.ksharma.krail.core.permission

sealed class PermissionStatus {
    data object NotDetermined : PermissionStatus()
    data object Granted : PermissionStatus()
    sealed class Denied : PermissionStatus() {
        data object Temporary : Denied()
        data object Permanent : Denied()
    }
}
