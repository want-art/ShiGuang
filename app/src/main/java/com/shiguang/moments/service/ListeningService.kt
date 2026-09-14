package com.shiguang.moments.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.shiguang.moments.AppGraph
import com.shiguang.moments.listener.parse.MessageParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * 通知监听：不闯入聊天界面，只在通知到达时识别美好瞬间。
 * 每次回调解析 → 判分 → 提示/自动收藏。真正的常驻由 KeepAliveService 兜底。
 */
class ListeningService : NotificationListenerService() {

    companion object {
        private const val TAG = "ListeningService"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val burst = RecentBurst(3 * 60 * 1000L)

        /** 模拟注入入口（debug 页面）：把一条 ChatMessage 直接送入同一管线 */
        fun inject(sender: String, app: String, text: String, type: com.shiguang.moments.data.models.MomentType) {
            val msg = com.shiguang.moments.data.models.ChatMessage(
                app = app, sender = sender, text = text, type = type,
                sentAt = System.currentTimeMillis(),
            )
            scope.launch {
                val profile = AppGraph.profileStore.profile.first()
                val isStar = msg.sender in profile.starSetFor(app)
                val burstCount = burst.recordAndCount("$app$sender", msg.sentAt)
                AppGraph.repo.ingest(msg, isStar, burstCount)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val msg = MessageParser.parse(applicationContext, sbn) ?: return
        val profile = try {
            runBlocking { AppGraph.profileStore.profile.first() }
        } catch (t: Throwable) { return }
        if (msg.app !in profile.trackedApps) return
        scope.launch {
            val isStar = msg.sender in profile.starSetFor(msg.app)
            val burstCount = burst.recordAndCount("${msg.app}${msg.sender}", msg.sentAt)
            val tier = AppGraph.repo.ingest(msg, isStar, burstCount)
            Log.d(TAG, "[${msg.app}]${msg.sender}: ${msg.text.take(30)} -> $tier (burst=$burstCount)")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) { /* no-op */ }
}

/** 连发计数：同一人 3 分钟内消息条数 */
class RecentBurst(private val windowMs: Long) {
    private val map = HashMap<String, MutableList<Long>>()
    @Synchronized
    fun recordAndCount(key: String, ts: Long): Int {
        val list = map.getOrPut(key) { mutableListOf() }
        val cutoff = ts - windowMs
        list.removeAll { it < cutoff }
        list.add(ts)
        return list.size
    }
}