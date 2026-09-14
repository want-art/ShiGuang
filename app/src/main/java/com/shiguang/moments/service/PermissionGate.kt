package com.shiguang.moments.service

import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat

object PermissionGate {
    fun hasNotificationAccess(ctx: Context): Boolean {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(ctx)
        return enabled.contains(ctx.packageName)
    }

    fun hasMediaRead(ctx: Context): Boolean {
        val perm = if (Build.VERSION.SDK_INT >= 33) android.Manifest.permission.READ_MEDIA_IMAGES
        else android.Manifest.permission.READ_EXTERNAL_STORAGE
        return ContextCompat.checkSelfPermission(ctx, perm) == PackageManager.PERMISSION_GRANTED
    }

    /** 相册归集真正可用（权限 + 系统 API） */
    fun canImportGallery(ctx: Context): Boolean = hasMediaRead(ctx)
}