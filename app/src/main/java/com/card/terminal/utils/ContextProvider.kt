package com.card.terminal.utils

import android.content.Context

object ContextProvider {
    private var appContext: Context? = null

    fun setApplicationContext(context: Context) {
        appContext = context.applicationContext
    }

    fun getApplicationContext(): Context {
        if (appContext == null) {
            throw IllegalStateException("Application context not set.")
        }
        return appContext!!
    }
}