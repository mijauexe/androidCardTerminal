package com.card.terminal.http.tasks

import com.card.terminal.utils.larusUtils.LarusFunctions
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask

class LarusCheckScansTask(val larusFunctions: LarusFunctions) :
    TimerTask() {
    private var timer: Timer? = null
    val delay = 0L
    val period = 1000L
    var started = false

    override fun run() {
        started = true
        larusFunctions.readLatestEvent()
    }

    fun startTask() {
        timer = Timer()
        timer?.scheduleAtFixedRate(
            this,
            delay,
            period
        )
        started = true
        Timber.d("Msg: LarusCheckScansTask started")
    }

    fun stopTask() {
        started = false
        timer?.cancel()
        Timber.d("Msg: LarusCheckScansTask stopped")
    }
}