package com.card.terminal

import android.app.AlarmManager

import android.app.PendingIntent
import android.content.Context

import android.content.Intent
import timber.log.Timber


class UEHandler(context: Context, restartActivityClass: Class<*>) :
    Thread.UncaughtExceptionHandler {
    private val mContext: Context
    private val mRestartActivityClass: Class<*>

    init {
        mContext = context
        mRestartActivityClass = restartActivityClass
    }

    override fun uncaughtException(thread: Thread, ex: Throwable) {
        val intent = Intent(mContext, mRestartActivityClass)
        val pendingIntent =
            PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val alarmManager = mContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.RTC, System.currentTimeMillis() + 2000] = pendingIntent
        println(thread.id)
        println(thread.name)
        println(thread.stackTrace)
        println(ex.stackTrace)
        println(ex.message)
        Timber.d("UncaughtException: %s | %s", ex.message, ex.cause)
        System.exit(2)
    }
}