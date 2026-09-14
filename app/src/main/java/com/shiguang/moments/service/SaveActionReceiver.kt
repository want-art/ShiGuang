package com.shiguang.moments.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.shiguang.moments.AppGraph
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** 一键收藏：从「值得记录」通知点按触发，把待办 Prompt 落库为瞬间 */
class SaveActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pid = intent.getStringExtra("pid") ?: return
        AppGraph.init(context)
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppGraph.repo.finishPrompt(pid)
        }
    }
}