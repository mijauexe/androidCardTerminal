package com.card.terminal

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.smartcardio.Card
import android.smartcardio.CardException
import android.smartcardio.CardTerminal
import android.smartcardio.CommandAPDU
import android.widget.Toast
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.lang.ref.WeakReference
import java.nio.charset.Charset

internal class GetCardStatusTask(demoActivity: MainActivity) :
    AsyncTask<CardTerminal, CardCommunicationProgress, Void>() {

    private val demoActivityRef: WeakReference<MainActivity> = WeakReference(demoActivity)

    public override fun doInBackground(vararg params: CardTerminal): Void? {
        val terminals = params.toList()
        while (!isCancelled) {
            try {
                terminals.forEach {
                    Timber.d("Waiting on terminal %s", it.name)
                    if (it.isCardPresent) {
                        Timber.d("Card present on terminal %s", it.name)
                        communicateWithCardAndUpdateUi(it)
                        Timber.d("Card can be removed from terminal %s", it.name)
                    }
                }
                Thread.sleep(USB_SLEEP_MILLIS)
            } catch (e: Exception) {
                Timber.e(e)
            }

        }
        return null
    }

    public override fun onProgressUpdate(vararg params: CardCommunicationProgress) {
        if (params.isNotEmpty()) {
            val cp = params[0]

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this.demoActivityRef.get()!!, cp.step.toString(), Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    /*
    public override fun onCancelled(unused: Void?) {
        demoActivityRef.get()?.clearUI()
    }
     */

    @Throws(CardException::class)
    private fun communicateWithCardAndUpdateUi(reader: CardTerminal) {
        val card = reader.connect("*")
        val listaBrojeva = parseResp(requestApdu(card))

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(demoActivityRef.get(), listaBrojeva.toString(), Toast.LENGTH_SHORT)
                .show()
        }

        card.disconnect(true)
        var isCardAbsent = false
        while (!isCancelled && !isCardAbsent) {
            isCardAbsent = reader.waitForCardAbsent(CARD_ABSENT_MILLIS)
            Thread.sleep(THREAD_SLEEP_MILLIS)
        }
    }

    @Throws(Exception::class)
    private fun requestApdu(card: Card): String {
        val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray("ff70076b07a005a10380010400"))
        val bytes = card.basicChannel.transmit(cmd).bytes
        return bytes?.let { ConvertUtils.byteArrayToString(it) }.toString()
    }


    @Throws(Exception::class)
    private fun parseResp(response: String): List<Int> {
        if (response.endsWith("9000")) {
            //var listica = hexStringToHexArray(resp)
            //9Dxx 03 06 03      81 25 8c 47 d0        9000
            var resp = response

            resp = resp.dropLast(4)
            resp = resp.substring(resp.indexOf("03"))
            val tbRemovedNum = Integer.parseInt(resp.substring(4, 6))
            resp = resp.drop(6)

            resp = ConvertUtils.hexStringToBinaryString(resp)
            resp = resp.dropLast(tbRemovedNum)

            if (resp.length == 26) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val cardNumber =
                    Integer.parseInt((Integer.parseInt(resp, 2) and 0x00FFFF).toString(), 2)
                val facilityCode = Integer.parseInt(resp.substring(1, 8), 2)
                return listOf(cardNumber, facilityCode)
            } else if (resp.length == 34) {
                val cardNumber = Integer.parseInt(resp.substring(17, resp.length - 1), 2)
                val facilityCode = Integer.parseInt(resp.substring(0, 17), 2)
                return listOf(cardNumber, facilityCode)
            } else if (resp.length == 37) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val facilityCode = Integer.parseInt(resp.substring(0, 16), 2)
                val cardNumber = Integer.parseInt(resp.substring(17), 2)
                return listOf(cardNumber, facilityCode)
            }
        }

        //37bit
        //1(000 0001 0010 0101 1)(000 1100 0100 0111 1101) 0               000
        //587 - fac code
        //50301 - card number od 17.bit do 35.

        //34bit
        //card number od 17.bit do length-1.
        //fac 0-16

        //26 bit
        //makni prvi i zadnji bit
        //fac - 1. do 8.
        //logicki - cijeli string and sa 0x00FFFF

        return listOf()
    }


    private fun getUid(card: Card): String {
        var uid: String? = null
        val response = card.sendAPDU(GET_UID_COMMAND)
        response?.let { uid = ConvertUtils.byteArrayToString(it, 2) }
        return uid.orEmpty()
    }

    private fun getReaderName(card: Card, cardTerminal: CardTerminal): String {
        val response = card.sendAPDU(GET_READER_NAME_COMMAND)
        if (response != null) {
            if (response.isNotEmpty()) {
                return try {
                    response.toString()
                } catch (e: UnsupportedEncodingException) {
                    cancel(true)
                    cardTerminal.name
                }

            }
        }
        return cardTerminal.name
    }

    private fun getCardAtr(card: Card): String {
        var atr: String? = null
        val respATR = card.atr
        respATR?.let { atr = ConvertUtils.byteArrayToString(it.bytes, 0) }
        return atr.orEmpty()
    }

    companion object {
        private const val GET_FIRMWARE_INFO_COMMAND = "FF70076B08A206A004A002960000"
        private const val GET_UID_COMMAND = "FFCA000000"
        private const val GET_READER_NAME_COMMAND = "FF70076B08A206A004A002820000"
        private const val CHARSET_ASCII = "ASCII"
        private const val USB_SLEEP_MILLIS = 200L
        private const val CARD_ABSENT_MILLIS = 100L
        private const val THREAD_SLEEP_MILLIS = 100L

    }

    fun Card.sendAPDU(request: String): ByteArray? {
        return try {
            val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray(request))
            val resp = basicChannel.transmit(cmd)
            resp.bytes
        } catch (e: CardException) {
            Timber.e(e)
            null
        }
    }
}
