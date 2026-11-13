package com.freekiosklite.data

import android.content.Intent
import android.content.pm.PackageManager

class AppLaunchResolver(private val packageManager: PackageManager) {
    fun launchIntent(packageName: String): Intent? =
        packageManager.getLaunchIntentForPackage(packageName)
}
