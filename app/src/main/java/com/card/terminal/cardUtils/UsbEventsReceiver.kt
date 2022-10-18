package com.card.terminal.cardUtils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbManager

class UsbEventsReceiver(
    private val onReaderAttach: () -> Unit = {},
    private val onReaderDetach: () -> Unit = {}
) : BroadcastReceiver() {
    private var isConnected = false

    override fun onReceive(context: Context, intent: Intent) {
        val isHasReader =
            (context.getSystemService(Context.USB_SERVICE) as UsbManager).deviceList.values.any {
                it.manufacturerName?.contains(READER_NAME) ?: false
            }

        when (intent.action) {
            READER_ATTACH_ACTION -> {
                if (isHasReader && !isConnected) {
                    onReaderAttach.invoke()
                    isConnected = true
                }
            }
            READER_DETACH_ACTION -> {
                if (!isHasReader && isConnected) {
                    onReaderDetach.invoke()
                    isConnected = false
                }
            }
        }
    }

    companion object {
        const val READER_ATTACH_ACTION = "com.hidglobal.ia.omnikey.intent.READER_ATTACHED"
        const val READER_DETACH_ACTION = "com.hidglobal.ia.omnikey.intent.READER_DETACHED"
        const val READER_NAME = "OMNIKEY"
    }
}