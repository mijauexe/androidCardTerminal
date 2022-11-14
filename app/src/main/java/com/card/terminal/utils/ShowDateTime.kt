package com.card.terminal.utils

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import java.time.LocalDateTime

object ShowDateTime {
    private lateinit var mutableDateTime: MutableLiveData<LocalDateTime>
    private lateinit var scope: CoroutineScope

    fun setDateAndTime(mut: MutableLiveData<LocalDateTime>) {
        this.mutableDateTime = mut
        mutableDateTime.postValue(LocalDateTime.now())

        stop()
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            var now = LocalDateTime.now()
            while (this.isActive) {
                now = now.plusSeconds(1)
                mutableDateTime.postValue(now)
                delay(1000)
            }
        }
    }

    fun stop() {
        try {
            if (this::scope.isInitialized && this.scope.isActive) {
                this.scope.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}