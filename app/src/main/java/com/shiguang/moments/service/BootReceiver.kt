package com.shiguang.moments.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.shiguang.moments.AppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 开机/更新后恢复守护服务（BOOT_COMPLETED 属于后台启动前台服务的豁免场景） */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        AppGraph.init(context)
        val enabled = try {
            runBlocking { AppGraph.profileStore.profile.first().listeningEnabled }
        } catch (t: Throwable) { false }
        if (!enabled) return
        try {
            val i = Intent(context, KeepAliveService::class.java)
            context.startForegroundService(i)
        } catch (t: Throwable) {
            Log.w("BootReceiver", "start keepalive failed", t)
        }
    }
}