package com.shiguang.moments

import android.content.Context
import com.shiguang.moments.data.repo.MomentRepository
import com.shiguang.moments.prefs.ProfileStore
import com.shiguang.moments.service.Notifier

/** 轻量服务定位器（避免引入 Hilt）。在 Application.onCreate 中初始化一次。 */
object AppGraph {
    var appContext: Context? = null; private set
    lateinit var profileStore: ProfileStore
        private set
    lateinit var repo: MomentRepository
        private set
    @Volatile private var inited = false

    fun init(ctx: Context) {
        if (inited) return
        synchronized(this) {
            if (inited) return
            appContext = ctx.applicationContext
            Notifier.initChannels(appContext!!)
            profileStore = ProfileStore(appContext!!)
            repo = MomentRepository(appContext!!)
            inited = true
        }
    }
}