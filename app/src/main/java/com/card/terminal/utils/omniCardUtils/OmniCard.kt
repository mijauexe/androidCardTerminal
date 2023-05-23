package com.card.terminal.utils.omniCardUtils

import android.app.Activity
import android.app.Application
import android.content.ComponentName
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.smartcardio.CardException
import android.smartcardio.CardTerminal
import android.smartcardio.TerminalFactory
import android.smartcardio.ipc.CardService
import android.smartcardio.ipc.ICardService
import androidx.lifecycle.MutableLiveData
import timber.log.Timber
import java.util.Timer
import java.util.TimerTask

object OmniCard {
    private const val SERVICE_ERROR = "cardService is null"
    private const val HAVE_NO_CONTEXT = "you need to bind first"
    private const val READER_NOT_FOUND = "reader not found"
    private const val READER_ATTACH = "reader attached"
    private const val READER_DETACH = "reader detached"

    private lateinit var usbEventsReceiver: UsbEventsReceiver
    private var isServiceConnected = false
    private var terminalFactory: TerminalFactory? = null
    private var cardService: ICardService? = null
    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null
    private var isRegisterUsbEventsReceiver: Boolean = false


    class Task() :
        TimerTask() {
        private var timer: Timer? = null
        val delay = 0L
        val period = 2000L
        var started = false

        override fun run() {
            started = true
            getCardStatus(false)
        }

        fun startTask() {
            timer = Timer()
            timer?.scheduleAtFixedRate(
                this,
                delay,
                period
            )
            started = true
            Timber.d("Msg: GetCardStatusTask started")
        }

        fun stopTask() {
            started = false
            timer?.cancel()
            Timber.d("Msg: GetCardStatusTask stopped")
        }
    }


    fun bindCardBackend(context: Activity, code: MutableLiveData<Map<String, String>>, isInit: Boolean) {
        mutableCode = code
        if (!isServiceConnected) {
            cardService = CardService.getInstance(context, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    Timber.e("onServiceConnected")
                    isServiceConnected = true
                    mContext = context.application
                    registerUsbEventReceiver()
                    getCardStatus(isInit)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    Timber.e("onServiceDisconnected")
                    isServiceConnected = false
                }
            })
        } else {
            mutableCode.postValue(mapOf("MESSAGE" to READER_ATTACH))
            getCardStatus()
        }
    }

    private fun getAvailableCardTerminals(): List<CardTerminal> {
        if (terminalFactory == null) {
            try {
                terminalFactory = cardService?.terminalFactory
            } catch (e: Exception) {
                Timber.e(e, "Unable to get terminal factory")
                return emptyList()
            }
        }

        try {
            terminalFactory?.terminals()?.list()?.let {
                return it.filterNotNull()
            }
        } catch (e: CardException) {
            Timber.e(e)
        }
        return emptyList()
    }

    private fun getCardStatus(isInit: Boolean = false) {
        val availableCardTerminals = getAvailableCardTerminals()
        if (cardService == null) {
            mutableCode.postValue(mapOf("MESSAGE" to SERVICE_ERROR))
            return
        }
        if (availableCardTerminals.isEmpty()) {
            if (!isInit) {
                mutableCode.postValue(mapOf("MESSAGE" to READER_NOT_FOUND))
            }
            return
        }
        GetCardStatusTask.execute(availableCardTerminals[0], mutableCode)
    }

    fun unbind() {
        GetCardStatusTask.stop()
    }

    fun release() {
        isServiceConnected = false
        unbind()
        cardService?.releaseService()
        unregisterUsbEventReceiver()
    }

    private fun registerUsbEventReceiver() {
        if (mContext == null) throw RuntimeException(HAVE_NO_CONTEXT)
        if (isRegisterUsbEventsReceiver) {
            return
        }
        val broadcastFilter = IntentFilter()
        broadcastFilter.addAction(UsbEventsReceiver.READER_DETACH_ACTION)
        usbEventsReceiver = UsbEventsReceiver(
            onReaderAttach = {
                getCardStatus()
            },
            onReaderDetach = {
                mutableCode.postValue(mapOf("MESSAGE" to READER_ATTACH))
                mutableCode.postValue(mapOf("MESSAGE" to READER_DETACH))
                unbind()
            }
        )
        mContext?.apply {
            registerReceiver(usbEventsReceiver, broadcastFilter)
            isRegisterUsbEventsReceiver = true
        }
    }

    private fun unregisterUsbEventReceiver() {
        try {
            mContext?.unregisterReceiver(usbEventsReceiver)
        } catch (e: Exception) {
            println(e)
        } finally {
            isRegisterUsbEventsReceiver = false
        }
    }
}