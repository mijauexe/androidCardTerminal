package com.card.terminal.utils.cardUtils

import android.smartcardio.Card
import android.smartcardio.CardException
import android.smartcardio.CardTerminal
import android.smartcardio.CommandAPDU
import androidx.lifecycle.MutableLiveData
import com.card.terminal.utils.cardUtils.ConvertUtils.hexStringToBinaryString
import kotlinx.coroutines.*
import timber.log.Timber

object GetCardStatusTask {
    private const val WILDCARD_PROTOCOL = "*"
    private const val READER_NAME = "OMNIKEY"
    private const val GET_UID_COMMAND = "FFCA000000"

    //private const val COMMAND_APDU = "ff680d0000"
    private const val COMMAND_APDU = "ff70076b07a005a10380010400"
    private const val CORRECT_APDU_END_RESPONSE = "9000"
    private const val SLEEP_MILLIS = 100L
    private const val MESSAGE = "MESSAGE"
    private lateinit var scope: CoroutineScope

    private var uid = ""
    private var atr = ""

    private lateinit var mutableCardCode: MutableLiveData<Map<String, String>>

    fun execute(
        terminal: CardTerminal,
        mutableCode: MutableLiveData<Map<String, String>>
    ) {
        stop()
        scope = CoroutineScope(Dispatchers.Default)
        mutableCardCode = mutableCode
        scope.launch {

            while (scope.isActive) {
                try {
                    if (READER_NAME in terminal.name && terminal.isCardPresent) {
                        val card = terminal.connect(WILDCARD_PROTOCOL)
                        uid = getCardUid(card)
                        atr = getCardAtr(card)
                        parseResp(requestApdu(card), mutableCode)

                        card.disconnect(true)
                        var isCardAbsent = false
                        while (scope.isActive && !isCardAbsent) {
                            isCardAbsent = terminal.waitForCardAbsent(SLEEP_MILLIS)
                            delay(SLEEP_MILLIS)
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
    private fun parseResp(response: String, mutableCode: MutableLiveData<Map<String, String>>) {
        if (response.endsWith(CORRECT_APDU_END_RESPONSE)) {
            var resp = response

            resp = resp.dropLast(4)
            resp = resp.substring(resp.indexOf("03"))
            val tbRemovedNum = Integer.parseInt(resp.substring(4, 6))
            resp = resp.drop(6)

            resp = hexStringToBinaryString(resp)
            resp = resp.dropLast(tbRemovedNum)

            var cardNumber = ""
            var facilityCode = ""

            val cardMap = mutableMapOf<String, String>()
            cardMap["CardFormat"] = resp.length.toString()
            when (resp.length) {
                26 -> {
                    resp = resp.dropLast(1)
                    resp = resp.drop(1)
                    cardNumber =
                        Integer.parseInt((Integer.parseInt(resp, 2) and 0x00FFFF).toString())
                            .toString()
                    facilityCode = Integer.parseInt(resp.substring(1, 8), 2).toString()
                }
                34 -> {
                    cardNumber = Integer.parseInt(resp.substring(17, resp.length - 1), 2).toString()
                    facilityCode = Integer.parseInt(resp.substring(0, 17), 2).toString()
                }
                37 -> {
                    resp = resp.dropLast(1)
                    resp = resp.drop(1)
                    facilityCode = Integer.parseInt(resp.substring(0, 16), 2).toString()
                    cardNumber = Integer.parseInt(resp.substring(17), 2).toString()
                }
                else -> {
                    mutableCode.postValue(mapOf("CardNumber" to "", "FacilityCode" to ""))
                    return
                }
            }
            cardMap["CardResponse"] = response
            cardMap["CardNumber"] = cardNumber
            cardMap["FacilityCode"] = facilityCode
            cardMap["UID"] = uid
            cardMap["ErrorCode"] = "0"
            cardMap["ATR"] = atr
            mutableCode.postValue(cardMap)
        } else {
            val cardMap = mutableMapOf<String, String>()
            cardMap["ErrorCode"] = "1"
            cardMap["CardResponse"] = response
            mutableCode.postValue(cardMap)
        }
    }

    @Throws(Exception::class)
    private fun getCardAtr(card: Card): String {
        var atr: String? = null
        val respATR = card.atr
        respATR?.let { atr = ConvertUtils.byteArrayToString(it.bytes) }
        return atr.orEmpty()
    }

    private fun getCardUid(card: Card): String {
        var uid: String? = null
        val response = card.sendAPDU(GET_UID_COMMAND)
        response?.let { uid = ConvertUtils.byteArrayToString(it, 2) }
        return uid.orEmpty()
    }

    private fun Card.sendAPDU(request: String): ByteArray? {
        return try {
            val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray(request))
            val resp = basicChannel.transmit(cmd)
            resp.bytes
        } catch (e: CardException) {
            Timber.e(e)
            null
        }
    }

    @Throws(Exception::class)
    private fun requestApdu(card: Card): String {
        val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray(COMMAND_APDU))
        val bytes = card.basicChannel.transmit(cmd).bytes
        return bytes?.let { ConvertUtils.byteArrayToString(it) }.toString()
    }
}