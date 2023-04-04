package com.card.terminal

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import timber.log.Timber


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val myIntent = Intent(context, MainActivity::class.java)
        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        myIntent.putExtra("boot", "yes")
        val b = Bundle()
        b.putString("boot", "yes")
        Timber.d("starting on boot?")
        context.startActivity(myIntent, b)
    }
}