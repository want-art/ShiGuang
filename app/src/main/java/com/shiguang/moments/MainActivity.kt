package com.shiguang.moments

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shiguang.moments.service.KeepAliveService
import com.shiguang.moments.service.Notifier
import com.shiguang.moments.service.PermissionGate
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
        ensureGuard()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
        ensureGuard()
    }

    private fun handleIntent(intent: Intent?) {
        val route = intent?.getStringExtra(Notifier.EXTRA_ROUTE) ?: return
        AppRouting.target.value = when {
            route == "moment" -> "moment/${intent.getLongExtra("moment_id", 0)}"
            else -> route
        }
    }

    /** 应用在前台就绪时，若已授权通知监听且开启守护，恢复保活服务 */
    private fun ensureGuard() {
        val enabled = try {
            runBlocking { AppGraph.profileStore.profile.first().listeningEnabled }
        } catch (t: Throwable) { false }
        if (!enabled) return
        if (!PermissionGate.hasNotificationAccess(this)) return
        try {
            val i = Intent(this, KeepAliveService::class.java)
            startForegroundService(i)
        } catch (_: Throwable) { }
    }
}