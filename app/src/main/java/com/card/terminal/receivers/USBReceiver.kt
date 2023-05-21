package com.card.terminal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import timber.log.Timber


class USBReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        val action = intent.action
        if (action != null) {
            if (action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Timber.d("USB device attached: " + usbDevice!!.deviceName)
            } else if (action == UsbManager.ACTION_USB_DEVICE_DETACHED) {
                val usbDevice = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Timber.d("USB device detached: " + usbDevice!!.deviceName)
            }
        }
    }
}