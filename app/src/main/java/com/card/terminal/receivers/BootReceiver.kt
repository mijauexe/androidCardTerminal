package com.card.terminal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.card.terminal.MainActivity
import timber.log.Timber


class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("Msg: BootReceiver started the app: %s | %s", context.toString(), intent.toString())

        // Create a Handler with the main thread's Looper
        val handler = Handler(Looper.getMainLooper())

        // Define the delay in milliseconds
        val delayMillis = 45000L

        // Define the action to be performed after the delay
        val runnable = Runnable {
            // Create the intent for MainActivity
            val myIntent = Intent(context, MainActivity::class.java)
            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            myIntent.putExtra("boot", "yes")
            val b = Bundle()
            b.putString("boot", "yes")
            context.startActivity(myIntent, b)
        }

        // Schedule the action with the specified delay
        handler.postDelayed(runnable, delayMillis)
    }
}