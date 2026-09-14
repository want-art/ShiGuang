package com.shiguang.moments.reminder

import android.content.Context
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.shiguang.moments.AppGraph
import com.shiguang.moments.service.Notifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val WORK_NAME = "daily_review"

    /** 每天固定时刻推「今晚想抽一段回忆吗」。用 24h 周期 + initialDelay 对齐到下一时刻。 */
    fun schedule(ctx: Context) {
        try {
            val (on, hour, minute) = try {
                val p = runBlocking { AppGraph.profileStore.profile.first() }
                Triple(p.dailyReviewEnabled, p.dailyReviewHour, p.dailyReviewMinute)
            } catch (t: Throwable) { Triple(true, 21, 30) }

            val req = PeriodicWorkRequestBuilder<DailyReviewWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(delayToNext(hour, minute), TimeUnit.MILLISECONDS)
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.REPLACE, req,
            )
            if (!on) WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
        } catch (t: Throwable) {
            Log.w("ReminderScheduler", "schedule failed", t)
        }
    }

    fun cancel(ctx: Context) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_NAME)
    }

    private fun delayToNext(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        return (next.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
    }
}

class DailyReviewWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        return try {
            AppGraph.init(applicationContext)
            val p = runBlocking { AppGraph.profileStore.profile.first() }
            val count = runBlocking { AppGraph.repo.countMoments() }
            if (!p.dailyReviewEnabled || count == 0L) return Result.success()
            Notifier.postReview(applicationContext)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}