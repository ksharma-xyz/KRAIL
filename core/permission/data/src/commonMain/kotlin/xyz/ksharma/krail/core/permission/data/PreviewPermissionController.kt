package xyz.ksharma.krail.core.permission.data

import xyz.ksharma.krail.core.permission.AppPermission
import xyz.ksharma.krail.core.permission.PermissionResult
import xyz.ksharma.krail.core.permission.PermissionStatus

/**
 * Stub [PermissionController] returned when a Composable is rendering inside
 * `LocalInspectionMode` (i.e. the IDE preview pane). Reports every permission as
 * already granted so previews don't have to reason about the request flow, and
 * makes [openAppSettings] a no-op because previews can't reach Android Settings.
 *
 * Used in place of the real `AndroidPermissionController` / `IosPermissionController`
 * during preview rendering only — production paths never see this object.
 */
internal object PreviewPermissionController : PermissionController {
    override suspend fun requestPermission(permission: AppPermission): PermissionResult =
        PermissionResult.Granted

    override suspend fun checkPermissionStatus(permission: AppPermission): PermissionStatus =
        PermissionStatus.Granted

    override fun wasPermissionRequested(permission: AppPermission): Boolean = true

    override fun openAppSettings() = Unit
}
