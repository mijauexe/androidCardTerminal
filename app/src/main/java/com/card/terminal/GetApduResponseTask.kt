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
import java.lang.ref.WeakReference

internal class GetApduResponseTask(demoActivity: MainActivity) : AsyncTask<CardTerminal, CardCommunicationProgress, Void>() {

    private val demoActivityRef: WeakReference<MainActivity> = WeakReference(demoActivity)

    override fun doInBackground(vararg params: CardTerminal): Void? {

        val terminal = params[0]
        try {
            //publishProgress(CardCommunicationProgress(CardCommunicationStep.CLEAR_UI))
            if (terminal.isCardPresent) {
                Timber.d("Card present on terminal %s", terminal.name)
                communicateWithCardAndUpdateUi(terminal)
                Timber.d("Card can be removed from terminal %s", terminal.name)
            } else {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(this.demoActivityRef.get(), "no_card_detected", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    public override fun onProgressUpdate(vararg params: CardCommunicationProgress) {
        if (params.isNotEmpty()) {
            val cp = params[0]

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this.demoActivityRef.get(), cp.step.toString(), Toast.LENGTH_LONG).show()
            }
        }
    }

    @Throws(CardException::class)
    private fun communicateWithCardAndUpdateUi(reader: CardTerminal) {
        //publishProgress(CardCommunicationProgress(CardCommunicationStep.CARD_STATUS, demoActivityRef.get()?.getString(R.string.card_present).orEmpty()))
        val card = reader.connect("*")
       // publishProgress(CardCommunicationProgress(CardCommunicationStep.CARD_STATUS, demoActivityRef.get()?.getString(R.string.getting_data).orEmpty()))
       // publishProgress(CardCommunicationProgress(CardCommunicationStep.APDU_RESPONSE, getApduResponse(card).toString()))
        println(getApduResponse(card))

        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this.demoActivityRef.get(), getApduResponse(card).toString(), Toast.LENGTH_LONG).show()
        }
       // publishProgress(CardCommunicationProgress(CardCommunicationStep.CARD_STATUS))
        card.disconnect(true)
    }


    private fun getApduResponse(card: Card): String? {
        var response = ""
        var request = ""
        var bytesToGetPACS = byteArrayOf(0xFF.toByte(), 0x70.toByte(), 0x07.toByte(), 0x6B.toByte(), 0x07.toByte(), 0xA0.toByte(), 0x05.toByte(), 0xA1.toByte(), 0x03.toByte(), 0x80.toByte(), 0x01.toByte(), 0x04.toByte(), 0x00.toByte()).toString()
        bytesToGetPACS = "FF70076B07A005A10380010400"
        request = bytesToGetPACS
        request.let {
            try {
                response = card.sendAPDU(it)?.let { it1 -> ConvertUtils.byteArrayToString(it1, 0) }.toString()
            } catch (e: NumberFormatException) {
                response = "invalid_apdu"
                // response = request
                Timber.e(e)
            } catch (l: IllegalArgumentException) {
                response = "invalid_apdu_length"
                Timber.e(l)
            }
        }
//        responsic = response
        return response
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
