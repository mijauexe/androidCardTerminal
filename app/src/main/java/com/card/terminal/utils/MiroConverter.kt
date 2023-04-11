package com.card.terminal.utils

import android.os.Bundle
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Card
import com.card.terminal.db.entity.Event
import com.card.terminal.db.entity.Person
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import timber.log.Timber

class MiroConverter {
    data class HOLDERS(
        val B0_CLASS: String,
        val B0_ID: String,
        val LNAME: String,
        val FNAME: String,
        val IMAGE1: ByteArray
    )

    data class CARDS(
        val B0_CLASS: String,
        val HOLDER_B0_ID: String,
        val CN: String
    )

    data class ACC_LEVELS_DISTR(
        val B0_CLASS: String,
        val HOLDER_B0_ID: String,
        val ACC_L_B0_ID: String,
        val EXPIRATION_DATE: String,
        val ACTIVATION_DATE: String
    )

    data class ifTermAddResponse(
        val counter: Int,
        val err: String,
        val msg: String
    )

    data class CREAD(
        val CN: String,
        val GENT: String,
        val ECODE: String,
        val DEV_B0_ID: String
    )

    data class EventStringPair(
        val eventList: List<Event>,
        val eventString: String
    )

    data class NewEventRequest( //saljem serveru evente
        val ACT: String,
        val IFTTERM2_B0_ID: String,
        val CREAD: List<CREAD>
    )

    data class serverRequestObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("HOLDERS") val HOLDERS: ArrayList<HOLDERS>,
        @SerializedName("CARDS") val CARDS: ArrayList<CARDS>,
        @SerializedName("ACC_LEVELS_DISTR") val ACC_LEVELS_DISTR: ArrayList<ACC_LEVELS_DISTR>
    )

    suspend fun convertFromServerRequest(something: String): String {
        val scope = CoroutineScope(Dispatchers.IO)

        if (something.contains("ADD_INIT1")) {
            val scope1 = CoroutineScope(Dispatchers.IO)
            val responseDeferred = scope1.async {
                val db = AppDatabase.getInstance(ContextProvider.getApplicationContext())
                db.clearAllTables()
            }
            val response = responseDeferred.await()
        }

        val responseDeferred = scope.async {
            val objectic = Gson().fromJson(something, serverRequestObject::class.java)

            Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
            when (objectic.ACT) {
                "ADD_HCAL" -> {
                    addHcal(objectic)
                }
                "ADD_INIT1" -> {
                    addHcal(objectic)
                }
                else -> {
                    Timber.d("Msg: unknown request: %s", something)
                }
            }
        }

        val response = responseDeferred.await()
        return ifTermResponse(response as ifTermAddResponse)
    }

    fun ifTermResponse(response: ifTermAddResponse): String {
        return "{\"ACT\": \"IFTSRV2_RESPONSE\",\"NUM_CREAD\": \"${response.counter}\",\"ERROR\": {\"CODE\": \"${response.err}\",\"TEXT\": \"${response.msg}\"}}"
    }

    fun addHcal(objectic: serverRequestObject): ifTermAddResponse {
        var counter = 0
        val personList = mutableListOf<Person>()
        for (person in objectic.HOLDERS) {
            personList.add(
                Person(
                    uid = person.B0_ID.toInt(),
                    classType = person.B0_CLASS,
                    firstName = person.FNAME,
                    lastName = person.LNAME,
                    image = person.IMAGE1
                )
            )
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.PersonDao().insertAll(personList)
            counter += personList.size
        } catch (e: Exception) {
            Timber.d("Exception while putting persons in db: %s | %s", e.message, e.cause)
        }

        val acList = mutableListOf<Int>()
        for (obj in objectic.ACC_LEVELS_DISTR) {
            acList.add(obj.ACC_L_B0_ID.toInt())
        }

        val cardList = mutableListOf<Card>()
        var i = 0

        for (obj in objectic.CARDS) {
            cardList.add(
                Card(
                    cardNumber = obj.CN,
                    classType = obj.B0_CLASS,
                    owner = obj.HOLDER_B0_ID.toInt(),
                    expirationDate = "",
                    accessLevel = acList[i]
                )
            ) //TODO EXPIRATION DATE
            i++
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.CardDao().insertAll(cardList)
            counter += cardList.size
        } catch (e: Exception) {
            Timber.d("Exception while putting persons in db: %s | %s", e.message, e.cause)
        }
        return ifTermAddResponse(counter = cardList.size, err = "0", msg = "Successful")
    }

    fun convertToNewEventFormat(cardResponse: Bundle): String {
        var eCode = 0 //TODO ECODE
        when (cardResponse.get("selection")) {
            "BE-TO" -> eCode = 0
            "Liječnik" -> eCode = 3
            "Privatno" -> eCode = 4
            "Pauza" -> eCode = 5
            "Poslovno" -> eCode = 6
        }

        var strNew = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"hep1_sisak\",\"CREAD\":["
        strNew += "{\"CN\":\"${cardResponse.get("CardCode")}\", \"GENT\":\"${cardResponse.get("DateTime")}\", \"ECODE\":\"0\", \"DEV_B0_ID\":\"0\"}]}"

//        val scope = CoroutineScope(Dispatchers.IO)
//        val responseDeferred = scope.async {
//            val db = AppDatabase.getInstance(ContextProvider.getApplicationContext())
//            val unpublishedEvents = db.EventDao().getUnpublishedEvents()
//            for (ue in unpublishedEvents.indices) {
//                if (ue != unpublishedEvents.size - 1) {
//                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\":\"$eCode\", \"DEV_B0_ID\":\"0\"},"
//                } else {
//                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\":\"$eCode\", \"DEV_B0_ID\":\"0\"}]}"
//                }
//            }
//        }
//        val response = responseDeferred.await()
        return strNew
    }

    suspend fun getFormattedUnpublishedEvents(): EventStringPair {
        var eCode = 0 //TODO ECODE HEP
        var strNew = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"hep1_sisak\",\"CREAD\":["
        val unpublishedEvents = mutableListOf<Event>()
        val scope = CoroutineScope(Dispatchers.IO)
        //TODO ECODE
        val responseDeferred = scope.async {
            val db = AppDatabase.getInstance(ContextProvider.getApplicationContext())
            unpublishedEvents.addAll(db.EventDao().getUnpublishedEvents())
            for (ue in unpublishedEvents.indices) {
                if (ue != unpublishedEvents.size - 1) {
                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\":\"0\", \"DEV_B0_ID\":\"0\"},"
                } else {
                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\":\"0\", \"DEV_B0_ID\":\"0\"}]}"
                }
            }
        }
        responseDeferred.await()
        return EventStringPair(unpublishedEvents, strNew)
    }

    fun convertECode(s: String): Int {
        var eCode = -1
        when (s) {
            s -> eCode = 0
//            "Liječnik" -> eCode = 3
//            "Privatno" -> eCode = 4
//            "Pauza" -> eCode = 5
//            "Poslovno" -> eCode = 6
        }
        return eCode
    }
}
