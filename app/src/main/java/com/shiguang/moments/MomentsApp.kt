package com.shiguang.moments

import android.app.Application
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MomentsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
        installCrashLogger()
    }

    /** 把崩溃堆栈写入 filesDir/crash.log，便于无 USB 时也能看到闪退原因 */
    private fun installCrashLogger() {
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val f = File(filesDir, "crash.log")
                val sb = StringBuilder()
                sb.appendLine("=== " + SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA).format(Date()) + " ===")
                sb.appendLine("thread=" + thread.name)
                sb.appendLine(throwable.toString())
                throwable.stackTrace.take(24).forEach { sb.appendLine("\tat $it") }
                f.appendText(sb.toString())
            } catch (_: Throwable) { }
            // 继续走系统默认处理（保留原生闪退提示）
            prev?.uncaughtException(thread, throwable)
        }
    }
}