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

/**
 * 通知出口（仅手动收藏确认 + 每日回忆推送）。
 * 已经去掉所有自动监听相关通知，保持克制。
 */
object Notifier {
    const val CH_SAVED = "ch_saved"
    const val CH_REVIEW = "ch_review"
    const val CH_CAPSULE = "ch_capsule"
    const val EXTRA_ROUTE = "sg_route"

    fun initChannels(ctx: Context) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        fun ch(id: String, name: String, imp: Int) {
            if (nm.getNotificationChannel(id) == null) {
                nm.createNotificationChannel(NotificationChannel(id, name, imp))
            }
        }
        ch(CH_SAVED, "收藏确认", NotificationManager.IMPORTANCE_DEFAULT)
        ch(CH_REVIEW, "每日回忆", NotificationManager.IMPORTANCE_DEFAULT)
        ch(CH_CAPSULE, "时光宝盒", NotificationManager.IMPORTANCE_HIGH)
    }

    private fun canPost(ctx: Context): Boolean = NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun openRoute(ctx: Context, route: String, momentId: Long? = null): PendingIntent {
        val i = Intent(ctx, MainActivity::class.java)
            .apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_ROUTE, route)
                if (momentId != null) putExtra("moment_id", momentId)
            }
        val req = (route.hashCode() shl 4) + (momentId?.toInt() ?: 0)
        return PendingIntent.getActivity(ctx, req, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** 手动收藏后轻提示 */
    fun postSaved(ctx: Context, momentId: Long, sender: String) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CH_SAVED)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("已收进回忆库 💙")
            .setContentText("一段与 $sender 的美好已被珍藏")
            .setContentIntent(openRoute(ctx, "moment", momentId))
            .setAutoCancel(true)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify("saved", momentId.toInt(), notif)
    }

    /** 每日回忆 */
    fun postReview(ctx: Context) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CH_REVIEW)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("今晚想抽一段回忆吗？")
            .setContentText("翻开一张卡片，回到某个被记住的瞬间 ✨")
            .setContentIntent(openRoute(ctx, "lucky"))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify("review", 787, notif)
    }

    /** 时光宝盒揭示：回到某个被预约的瞬间 */
    fun postCapsule(ctx: Context, momentId: Long, sender: String) {
        if (!canPost(ctx)) return
        val notif = NotificationCompat.Builder(ctx, CH_CAPSULE)
            .setSmallIcon(R.drawable.ic_notif_moment)
            .setContentTitle("时光宝盒打开啦 📦")
            .setContentText("曾经与 $sender 的那段悄悄回来见你了")
            .setContentIntent(openRoute(ctx, "moment", momentId))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify("capsule_$momentId", momentId.toInt(), notif)
    }
}