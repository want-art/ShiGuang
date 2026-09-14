package com.shiguang.moments.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.shiguang.moments.MainActivity
import com.shiguang.moments.R
import com.shiguang.moments.data.models.ChatMessage

/**
 * 通知出口。所有通知都指向 App 内部页面或收藏广播。
 * 关键点：内容本身是用户自己的聊天通知已有内容，这里只做软提示，不额外侵犯隐私。
 */
object Notifier {
    const val CH_PROMPT = "ch_prompt"
    const val CH_SAVED = "ch_saved"
    const val CH_REVIEW = "ch_review"
    const val CH_KEEPALIVE = "ch_keepalive"
    const val EXTRA_ROUTE = "sg_route"
    const val ROUTE_LUCKY = "lucky"
    const val ROUTE_MAIN = "main"

    fun initChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fun ch(id: String, name: String, imp: Int) {
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(NotificationChannel(id, name, imp))
            }
        }
        ch(CH_PROMPT, "值得记录的瞬间", NotificationManager.IMPORTANCE_HIGH)
        ch(CH_SAVED, "收藏确认", NotificationManager.IMPORTANCE_DEFAULT)
        ch(CH_REVIEW, "每日回忆", NotificationManager.IMPORTANCE_DEFAULT)
        ch(CH_KEEPALIVE, "美好瞬间守护", NotificationManager.IMPORTANCE_MIN)
    }

    private fun canPost(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    fun openRoute(ctx: Context, route: String, momentId: Long? = null): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_ROUTE, route)
                if (momentId != null) putExtra("moment_id", momentId)
            }
        val req = (route.hashCode() shl 4) + (momentId?.toInt() ?: 0)
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** 「值得记录？」轻提示，点按即为一键收藏 */
    fun postPrompt(ctx: Context, pid: String, msg: ChatMessage) {
        if (!canPost(ctx)) return
        val save = PendingIntent.getBroadcast(
            ctx, pid.hashCode(),
            Intent(ctx, SaveActionReceiver::class.java).setAction("save").putExtra("pid", pid),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(ctx, CH_PROMPT)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("值得记录的瞬间")
            .setContentText("${msg.sender}：${snippet(msg.text, 60)}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("${msg.sender}：${msg.text}"))
            .setContentIntent(save)
            .addAction(0, "收藏它", save)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getManager(ctx).notify("prompt", pid.hashCode(), notif)
    }

    /** 已收藏 🌙 */
    fun postSaved(ctx: Context, momentId: Long, sender: String) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CH_SAVED)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("已收进回忆库 💙")
            .setContentText("一段与 ${sender} 的美好已被珍藏")
            .setContentIntent(openRoute(ctx, "moment", momentId))
            .setAutoCancel(true)
            .build()
        getManager(ctx).notify("saved", momentId.toInt(), notif)
    }

    /** 每日回忆 */
    fun postReview(ctx: Context) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CH_REVIEW)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("今晚想抽一段回忆吗？")
            .setContentText("翻开一张卡片，回到某个被记住的瞬间 ✨")
            .setContentIntent(openRoute(ctx, ROUTE_LUCKY))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        getManager(ctx).notify("review", 787, notif)
    }

    /** 无障碍捕获提示：屏幕上聚到一段像值得记录的长文 */
    fun postCapturePrompt(ctx: Context, capture: String) {
        if (!canPost(ctx)) return
        val i = Intent(ctx, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(EXTRA_ROUTE, "capture")
        }
        val pi = PendingIntent.getActivity(
            ctx, 42, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notif = NotificationCompat.Builder(ctx, CH_REVIEW)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("屏幕上有段文字值得留档？")
            .setContentText(capture.take(40) + "…")
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        try { getManager(ctx).notify(88, notif) } catch (t: Throwable) { }
    }

    /** 守护前台通知 */
    fun postKeepAlive(ctx: Context): Notification {
        val notif = NotificationCompat.Builder(ctx, CH_KEEPALIVE)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("拾光正在守护美好瞬间")
            .setContentText("回到聊天里，遇到想留住的就点一下收藏")
            .setContentIntent(openRoute(ctx, ROUTE_MAIN))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
        return notif
    }

    private fun getManager(ctx: Context) = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun snippet(s: String, max: Int): String =
        if (s.length <= max) s else s.take(max) + "…"
}