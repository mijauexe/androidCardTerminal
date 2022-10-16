package com.card.terminal

import android.app.Application
import android.content.Context

class App: Application() {
    var ctx: Context? = null

    override fun onCreate() {
        super.onCreate()
        ctx = applicationContext
    }

}