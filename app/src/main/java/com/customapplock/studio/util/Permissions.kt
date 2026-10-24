package com.customapplock.studio.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.customapplock.studio.service.LockMonitorService

data class PermState(
    val accessibility: Boolean,
    val overlay: Boolean,
    val notifications: Boolean,
    val camera: Boolean
) {
    val required: Boolean get() = accessibility && overlay
}

object Permissions {
    fun check(ctx: Context) = PermState(
        accessibility = isAccessibilityOn(ctx),
        overlay = Settings.canDrawOverlays(ctx),
        notifications = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED,
        camera = ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    )

    fun isAccessibilityOn(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val cn = ComponentName(ctx, LockMonitorService::class.java)
        return enabled.split(':').any {
            it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
        }
    }
}
