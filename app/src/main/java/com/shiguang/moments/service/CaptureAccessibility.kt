package com.shiguang.moments.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.shiguang.moments.AppGraph
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * 无障碍增强（用户可选授权）：当你在微信/QQ 聊天界面时，
 * 把当前会话的历史长文聚起来，若整体像「值得记录」，提示一键收入。
 * 不截屏、不读无关应用，只在启用时才工作。
 */
class CaptureAccessibility : AccessibilityService() {

    companion object {
        @Volatile var pendingCapture: String = ""
            private set
    }

    private val lines = LinkedHashSet<String>()
    private var lastLineAt = 0L
    private var lastPromptAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg != com.shiguang.moments.prefs.Profile.WEIXIN && pkg != com.shiguang.moments.prefs.Profile.QQ) return

        val enabled = try { runBlocking { AppGraph.profileStore.profile.first().a11yEnabled } } catch (t: Throwable) { false }
        if (!enabled) return

        val text = event.text?.joinToString("\n")?.trim()?.orEmpty() ?: return
        if (text.isEmpty()) return
        val now = System.currentTimeMillis()
        if (now - lastLineAt > 12_000) lines.clear()
        lastLineAt = now
        lines.add(text)
        if (lines.size > 120) lines.remove(lines.first())

        val joined = lines.joinToString("\n")
        if (joined.length >= 80 && now - lastPromptAt > 90_000) {
            lastPromptAt = now
            pendingCapture = joined
            Notifier.postCapturePrompt(applicationContext, joined)
        }
    }

    override fun onInterrupt() {}
}