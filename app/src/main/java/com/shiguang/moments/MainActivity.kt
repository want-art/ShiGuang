package com.shiguang.moments

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shiguang.moments.reminder.ReminderScheduler
import com.shiguang.moments.service.Notifier
import com.shiguang.moments.ui.AppRoot
import com.shiguang.moments.ui.theme.ShiguangTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** 通知打开 App 时携带的跳转目标 */
object AppRouting {
    val target = androidx.compose.runtime.mutableStateOf<String?>(null)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            ShiguangTheme { AppRoot() }
        }
        // 启动时拉起每日回忆提醒（WorkManager 会在进程驻留时按用户设置的时间推送）
        try {
            val enabled = runBlocking { AppGraph.profileStore.profile.first().dailyReviewEnabled }
            if (enabled) ReminderScheduler.schedule(this) else ReminderScheduler.cancel(this)
        } catch (_: Throwable) { }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra(Notifier.EXTRA_ROUTE) ?: return
        AppRouting.target.value = when {
            route == "moment" -> "moment/${intent.getLongExtra("moment_id", 0)}"
            else -> route
        }
    }
}