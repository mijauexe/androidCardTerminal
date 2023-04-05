package com.card.terminal.utils

import android.graphics.Bitmap
import android.os.Bundle
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Card
import com.card.terminal.db.entity.Person
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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

    data class ACC_LEVELS(
        val B0_CLASS: String,
        val HOLDER_B0_ID: String,
        val ACC_L_B0_ID: String
    )

    data class addAllObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("HOLDERS") val HOLDERS: ArrayList<HOLDERS>,
        @SerializedName("CARDS") val CARDS: ArrayList<CARDS>,
        @SerializedName("ACC_LEVELS") val ACC_LEVELS: ArrayList<ACC_LEVELS>
    )

    fun convertFromAddAll(something: String) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {

            var counter = 0
            val objectic = Gson().fromJson(something, addAllObject::class.java)

            print(objectic)

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
            for (obj in objectic.ACC_LEVELS) {
                acList.add(obj.ACC_L_B0_ID.toInt())
            }

            val cardList = mutableListOf<Card>()
            var i = 0

            for (obj in objectic.CARDS) {
                cardList.add(
                    Card(
                        cardNumber = obj.CN,
                        owner = obj.HOLDER_B0_ID.toInt(),
                        expirationDate = "",
                        accessLevel = acList.get(i)
                    )
                ) //TODO EXPIRATION DATE
                i++
            }

            try {
                val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
                db.CardDao().insertAll(cardList)
                counter += personList.size
            } catch (e: Exception) {
                Timber.d("Exception while putting persons in db: %s | %s", e.message, e.cause)
            }
        }
    }

fun convertToPOSTFormat(cardResponse: Bundle): String {
    var eCode = -1
    when (cardResponse.get("selection")) {
        "BE-TO" -> eCode = 0
        "Liječnik" -> eCode = 3
        "Privatno" -> eCode = 4
        "Pauza" -> eCode = 5
        "Poslovno" -> eCode = 6
    }

    val str = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"hep1-sisak\"," +
            "\"CREAD\":[" +
            "{\"CN\":\"${cardResponse.get("CardCode")}\", \"GENT\":\"${
                cardResponse.get(
                    "DateTime"
                )
            }\", \"ECODE\":\"${eCode}\", \"DEV_B0_ID\":\"${
                cardResponse.get(
                    "userId"
                )
            }\"}" +
            "]" +
            "}"

    print(str)
    return str
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
