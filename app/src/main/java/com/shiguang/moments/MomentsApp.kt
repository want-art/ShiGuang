package com.shiguang.moments

import android.app.Application

class MomentsApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}