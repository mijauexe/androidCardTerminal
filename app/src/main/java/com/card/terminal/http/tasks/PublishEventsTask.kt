package com.card.terminal.http.tasks

import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.larusUtils.LarusFunctions
import timber.log.Timber
import java.util.*

class PublishEventsTask() :
    TimerTask() {
    private var timer: Timer? = null
    val delay = 0L
    val period = 180000L
    var started = false

    override fun run() {
        started = true
        MyHttpClient.publishUnpublishedEvents()
    }

    fun startTask() {
        timer = Timer()
        timer?.scheduleAtFixedRate(
            this,
            delay,
            period
        )
        started = true
        Timber.d("Msg: PublishEventsTask started")
    }

    fun stopTask() {
        started = false
        timer?.cancel()
        Timber.d("Msg: PublishEventsTask stopped")
    }
}