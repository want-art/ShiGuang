package com.shiguang.moments.service

import android.app.Service
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import com.shiguang.moments.AppGraph
import com.shiguang.moments.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * 保活前台服务：
 * 1. 以 dataSync 前台服务抬高进程优先级，让通知监听基本不被厂商杀；
 * 2. 常驻相册图片归集观察器（用户在微信里长按保存的图 → 自动挂到对应瞬间）。
 */
class KeepAliveService : Service() {

    companion object {
        const val NOTIF_ID = 1001
        private const val TAG = "KeepAliveService"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var observer: MediaObserver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID, Notifier.postKeepAlive(this))
        ReminderScheduler.schedule(this)
        if (PermissionGate.hasMediaRead(this) && observer == null) {
            observer = MediaObserver(this).also { it.attach() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observer?.unregister()
        observer = null
        scope.cancel()
        super.onDestroy()
    }

    /** 相册新图观察 → 归集到最近未配图的图片瞬间 */
    private class MediaObserver(private val ctx: Context) : ContentObserver(null) {
        private val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        fun attach() = ctx.contentResolver.registerContentObserver(uri, true, this)
        fun unregister() = ctx.contentResolver.unregisterContentObserver(this)

        override fun onChange(selfChange: Boolean) { onChange(selfChange, null) }
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { processNewImages() }
        }

        private suspend fun processNewImages() {
            val resolver = ctx.contentResolver
            val now = System.currentTimeMillis()
            val cutoff = now - 10 * 60 * 1000L
            val cols = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
            )
            try {
                resolver.query(uri, cols, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")?.use { c ->
                    var matched = 0
                    while (c.moveToNext() && matched < 8) {
                        val id = c.getLong(0)
                        val name = c.getString(1) ?: continue
                        val added = c.getLong(2) * 1000L
                        if (added < cutoff) break
                        if (!looksLikeImage(name)) continue
                        val fileUri = ContentUris.withAppendedId(uri, id)
                        if (AppGraph.repo.attachGalleryImage(name, id.toString(), fileUri, added)) matched++
                    }
                }
            } catch (t: Throwable) {
                Log.w(TAG, "gallery scan failed", t)
            }
        }

        private fun looksLikeImage(name: String): Boolean {
            val lower = name.lowercase()
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") ||
                lower.endsWith(".webp") || lower.endsWith(".heic") || lower.endsWith(".gif")
        }
    }
}