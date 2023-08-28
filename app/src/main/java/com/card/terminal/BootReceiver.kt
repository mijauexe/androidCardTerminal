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
        Timber.d(
            "Msg: BootReceiver started the app: %s | %s",
            context.toString(),
            intent.toString()
        )
        context.startActivity(myIntent, b)


//        val i = Intent()
//        i.component = ComponentName(
//            "net.christianbeier.droidvnc_ng",
//            "net.christianbeier.droidvnc_ng.MainService"
//        )
//
//        val action = "ACTION_START" // or "ACTION_CONNECT" or "ACTION_STOP"
//        i.action = action
//        context.startService(i)
//        Timber.d("Msg: BootReceiver started the vnc server: %s | %s", context.toString(), intent.toString())
    }
}