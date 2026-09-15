package com.shiguang.moments.reminder

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shiguang.moments.AppGraph
import com.shiguang.moments.service.Notifier
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class CapsuleRevealWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return try {
            AppGraph.init(applicationContext)
            val mid = inputData.getLong(KEY_MID, 0L)
            if (mid == 0L) return Result.success()
            // 若到点瞬间已被撤回或已揭示，跳过
            val m = AppGraph.repo.dueCapsules().firstOrNull { it.id == mid }
            if (m == null) return Result.success()
            AppGraph.repo.markCapsuleRevealed(mid)
            // 通知 + 深链到 moment detail
            try {
                val ni = NotificationManagerCompat.from(applicationContext)
                if (ni.areNotificationsEnabled()) {
                    Notifier.postCapsule(applicationContext, m.id, m.sender)
                }
            } catch (t: Throwable) {
                Log.w("CapsuleWorker", "post notif failed", t)
            }
            Result.success()
        } catch (t: Throwable) {
            Log.w("CapsuleWorker", "reveal failed", t)
            Result.retry()
        }
    }

    companion object {
        private const val KEY_MID = "mid"
        private const val PREFIX = "capsule_reveal_"

        fun schedule(ctx: Context, momentId: Long, revealAt: Long) {
            val delayMs = (revealAt - System.currentTimeMillis()).coerceAtLeast(5_000L)
            val req = OneTimeWorkRequestBuilder<CapsuleRevealWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putLong(KEY_MID, momentId).build())
                .addTag(PREFIX + momentId)
                .build()
            WorkManager.getInstance(ctx).enqueueUniqueWork(
                "$PREFIX$momentId", ExistingWorkPolicy.REPLACE, req,
            )
        }

        fun cancel(ctx: Context, momentId: Long) {
            WorkManager.getInstance(ctx).cancelUniqueWork("$PREFIX$momentId")
        }
    }
}