package com.card.terminal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.card.terminal.BuildConfig
import com.card.terminal.utils.adamUtils.Adam6050D
import com.card.terminal.utils.adamUtils.DigitalOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class RelayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != null && intent.action.equals("com.relay.hold")) {
            performAction(1, 1)
            Timber.d("Got hold intent, set relay 2 to hold...")
        }
        if (intent.action != null && intent.action.equals("com.relay.pulse")) {
            performAction(1, 0)
            Timber.d("Got pulse intent, set relay 2 to pulse...")
        }
    }

    private fun performAction(relayNum: Int, active: Int) {
        val scope3 = CoroutineScope(Dispatchers.IO)
        scope3.launch {
            val ip = BuildConfig.adamIP
            val username = BuildConfig.adamUsername
            val password = BuildConfig.adamPassword

            val adam = Adam6050D(ip, username, password)
            val doOutput = DigitalOutput()

            try {
                doOutput[relayNum] = active
                adam.output(doOutput)
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
    }
}