package com.card.terminal.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.card.terminal.http.MyHttpClient
import timber.log.Timber


class RelayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != null && intent.action.equals("com.relay.hold")) {
            performAction(1)
            Timber.d("Got hold intent, set relay 2 to hold...")

        }
        if (intent.action != null && intent.action.equals("com.relay.pulse")) {
            performAction(0)
            Timber.d("Got pulse intent, set relay 2 to pulse...")
        }
    }

    private fun performAction(pulseOrHold: Int) {
//        val prefs = ContextProvider.getApplicationContext()
//            .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
//        val editor = prefs.edit()
//        editor.putInt("relay2State", pulseOrHold)
//        editor.commit()

//        MyHttpClient.relayMode(2, pulseOrHold)
        MyHttpClient.relayMode(2, 0)
    }
}