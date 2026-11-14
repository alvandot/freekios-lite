package com.freekiosklite.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context

class DevicePolicyController(context: Context) {
    private val devicePolicyManager =
        context.getSystemService(DevicePolicyManager::class.java)
    private val packageName = context.packageName

    fun isDeviceOwner(): Boolean = devicePolicyManager.isDeviceOwnerApp(packageName)

    fun canStartLockTask(): Boolean =
        isDeviceOwner() || devicePolicyManager.isLockTaskPermitted(packageName)

    fun startLockTask(activity: Activity): Result<Unit> {
        if (!canStartLockTask()) {
            return Result.failure(
                IllegalStateException("FreeKiosk Lite is not an allowed lock-task package"),
            )
        }
        activity.startLockTask()
        return Result.success(Unit)
    }
}
