package com.card.terminal.http.tasks

import com.card.terminal.utils.larusUtils.LarusFunctions
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
        timer = Timer()
        timer?.scheduleAtFixedRate(
            this,
            delay,
            period
        )
    }

    fun stopTask() {
        timer?.cancel()
    }
}