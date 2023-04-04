package com.card.terminal.http.tasks

import com.card.terminal.utils.larusUtils.LarusFunctions
import timber.log.Timber
import java.util.*

class LarusCheckScansTask(val larusFunctions: LarusFunctions) :
    TimerTask() {
    private var timer: Timer? = null
    val delay = 0L
    val period = 1000L

    override fun run() {
        larusFunctions.readLatestEvent()
    }

    fun startTask() {
        Timber.d("Msg: LarusCheckScansTask started")
        timer = Timer()
        timer?.scheduleAtFixedRate(
            this,
            delay,
            period
        )
    }

    fun stopTask() {
        Timber.d("Msg: LarusCheckScansTask stopped")
        timer?.cancel()
    }
}