package com.card.terminal
import android.smartcardio.Card
import android.smartcardio.CardTerminal
import android.smartcardio.CommandAPDU
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import androidx.lifecycle.MutableLiveData
import com.card.terminal.ConvertUtils.hexStringToBinaryString
import com.card.terminal.OmniCard.HAVE_NO_CARD
import kotlinx.coroutines.*

object GetCardStatusTask {
    private const val WILDCARD_PROTOCOL = "*"
    private const val READER_NAME = "OMNIKEY"
    //private const val COMMAND_APDU = "ff680d0000"
    private const val COMMAND_APDU = "ff70076b07a005a10380010400"
    private const val CORRECT_APDU_END_RESPONSE = "9000"
    private const val SLEEP_MILLIS = 100L

    private lateinit var scope: CoroutineScope
    private lateinit var mutableCode: MutableLiveData<Pair<OmniCard.Status, String>>

    fun execute(
        terminal: CardTerminal,
        mutableCode: MutableLiveData<Pair<OmniCard.Status, String>>
    ) {
        stop()
        scope = CoroutineScope(Dispatchers.Default)
        this.mutableCode = mutableCode
        scope.launch {
            var count = 0
            while (scope.isActive) {
                try {
                    if (!terminal.isCardPresent) {
                        if (count == 0) {
                            mutableCode.postValue(Pair(OmniCard.Status.MESSAGE, HAVE_NO_CARD))
                        }
                        count++
                    } else if (READER_NAME in terminal.name) {
                        count = 0
                        val card = terminal.connect(WILDCARD_PROTOCOL)
                        getCardAtr(card)
                        parseResp(requestApdu(card), mutableCode)
                        card.disconnect(true)
                        var isCardAbsent = false
                        while (scope.isActive && !isCardAbsent) {
                            isCardAbsent = terminal.waitForCardAbsent(SLEEP_MILLIS)
                            delay(SLEEP_MILLIS)
                        }
                        mutableCode.value?.apply {
                            if (second != HAVE_NO_CARD) {
                                mutableCode.postValue(Pair(OmniCard.Status.MESSAGE, HAVE_NO_CARD))
                            }
                        }
                    }
                    delay(SLEEP_MILLIS)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        try {
            if (this::scope.isInitialized && scope.isActive) {
                scope.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Throws(Exception::class)
    private fun parseResp(response: String, mutableCode: MutableLiveData<Pair<OmniCard.Status, String>>) {
        if (response.endsWith(CORRECT_APDU_END_RESPONSE)) {
            //var listica = hexStringToHexArray(resp)
            //9Dxx 03 06 03      81 25 8c 47 d0        9000
            var resp = response

            resp = resp.dropLast(4)
            resp = resp.substring(resp.indexOf("03"))
            val tbRemovedNum = Integer.parseInt(resp.substring(4, 6))
            resp = resp.drop(6)

            resp = hexStringToBinaryString(resp)
            resp = resp.dropLast(tbRemovedNum)

            if (resp.length == 26) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val cardNumber =
                    Integer.parseInt((Integer.parseInt(resp, 2) and 0x00FFFF).toString(), 2)
                val facilityCode = Integer.parseInt(resp.substring(1, 8), 2)
                mutableCode.postValue(Pair(OmniCard.Status.READ, listOf(cardNumber, facilityCode).toString()))
            } else if (resp.length == 34) {
                val cardNumber = Integer.parseInt(resp.substring(17, resp.length - 1), 2)
                val facilityCode = Integer.parseInt(resp.substring(0, 17), 2)
                mutableCode.postValue(Pair(OmniCard.Status.READ, listOf(cardNumber, facilityCode).toString()))
            } else if (resp.length == 37) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val facilityCode = Integer.parseInt(resp.substring(0, 16), 2)
                val cardNumber = Integer.parseInt(resp.substring(17), 2)
                mutableCode.postValue(Pair(OmniCard.Status.READ, listOf(cardNumber, facilityCode).toString()))
            }
        }
    }

    @Throws(Exception::class)
    private fun getCardAtr(card: Card): String {
        var atr: String? = null
        val respATR = card.atr
        respATR?.let { atr = ConvertUtils.byteArrayToString(it.bytes) }
        return atr.orEmpty()
    }

    @Throws(Exception::class)
    private fun requestApdu(card: Card): String {
        val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray(COMMAND_APDU))
        val bytes = card.basicChannel.transmit(cmd).bytes
        return bytes?.let { ConvertUtils.byteArrayToString(it) }.toString()
    }
}