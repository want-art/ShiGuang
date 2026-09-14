package com.shiguang.moments.listener.parse

import android.app.Notification
import android.content.Context
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.shiguang.moments.data.models.ChatMessage
import com.shiguang.moments.data.models.MomentType

/**
 * 通知 → 结构化聊天消息。
 * 微信/QQ 走同一套启发式（标题=联系人、文本=内容、消息标记判定类型），
 * 其他可读通知的 App 共享这套通用逻辑；后续可为某 App 独写 Parser 替换。
 */
object MessageParser {

    private val MARKERS = listOf("[图片]", "[语音]", "[视频]", "[动画表情]", "[表情]", "[文件]", "[位置]", "[链接]")
    private val NOISE_SENDERS = listOf("微信", "WeChat", "QQ", "腾讯", "腾讯新闻", "系统通知", "通知", "服务通知")

    fun parse(ctx: Context, sbn: StatusBarNotification): ChatMessage? {
        val pkg = sbn.packageName ?: return null
        val extras = sbn.notification.extras
        val appLabel = appLabel(ctx, pkg)

        val title = extras.getString(Notification.EXTRA_TITLE)?.trim().orEmpty()
        if (title.isEmpty()) return null

        var text = extras.getString(Notification.EXTRA_BIG_TEXT)?.trim()
            ?: extras.getString(Notification.EXTRA_TEXT)?.trim().orEmpty()

        // MessagingStyle 里能拿到更完整的最近一条
        NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(sbn.notification)?.let { style ->
            style.messages.lastOrNull()?.let { last ->
                val t = last.text?.toString()?.trim()
                if (!t.isNullOrEmpty()) text = t
            }
        }
        if (text.isEmpty()) text = extras.getString(Notification.EXTRA_SUB_TEXT)?.trim().orEmpty()
        if (text.isEmpty()) return null

        // 系统/自身推送（标题等于应用名，或已知噪声发送者）直接放掉
        if (title == appLabel || title in NOISE_SENDERS) return null

        val type = detectType(text)
        val clean = stripMarkers(text)
        return ChatMessage(
            app = pkg,
            sender = title,
            text = clean,
            type = type,
            sentAt = sbn.postTime.let { if (it == 0L) System.currentTimeMillis() else it },
        )
    }

    fun detectType(text: String): MomentType {
        return when {
            text.contains("[图片]") || text.contains("[视频]") || text.contains("[文件]") -> MomentType.IMAGE
            text.contains("[语音]") -> MomentType.VOICE
            text.contains("[动画表情]") || text.contains("[表情]") -> MomentType.STICKER
            stripMarkers(text).length >= 60 -> MomentType.LONG_TEXT
            else -> MomentType.TEXT
        }
    }

    fun stripMarkers(text: String): String {
        var t = text
        for (m in MARKERS) t = t.replace(m, "")
        t = t.replace(regex = Regex("""\[[一-龥A-Za-z0-9]{1,6}\]"""), replacement = "")
        return t.trim()
    }

    private fun appLabel(ctx: Context, pkg: String): String = try {
        ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
    } catch (_: Throwable) { pkg }
}