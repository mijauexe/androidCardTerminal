package com.card.terminal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.card.terminal.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


class AdamRelayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != null && intent.action.equals("com.relay.hold")) {
            performAction(1)
            Timber.d("Got hold intent, set relay to hold...")
        }
        if (intent.action != null && intent.action.equals("com.relay.pulse")) {
            performAction(0)
            Timber.d("Got pulse intent, set relay to pulse...")
        }
    }

    fun performAction(active: Int) {
        val scope3 = CoroutineScope(Dispatchers.IO)
        scope3.launch {
            val socket = DatagramSocket()
            socket.soTimeout = 2000

            /*https://portal.7thsense.one/user-guides/MC258-controlling-devices/index.html?dsc_appendix_sending_ascii_to_adam.html*/

            var binaryString = "011" //ne diraj

            //relej 0-5
            binaryString += ContextProvider
                .getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                .getInt("adamRelayNum", 0).toString()

            //pali=01, gasi=00
            binaryString += "0$active"
            val cmd = "#$binaryString\r"
            println(binaryString)

            val address = InetAddress.getByName(
                ContextProvider
                .getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                .getString("adamIP", "")
            )

            val packet = DatagramPacket(cmd.toByteArray(), cmd.length, address, 1025)
            socket.send(packet)
            socket.close()
        }
//            val adam = Adam6050D(ip!!, username!!, password!!)
//            val doOutput = DigitalOutput()
//            try {
//                Timber.d("sven1")
//                doOutput[relayNum] = active
//                adam.output(doOutput)
//                Timber.d("sven2")
//            } catch (e: Exception) {
//                Timber.d(e)
//            }
//        }
    }
}