package com.card.terminal.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.card.terminal.MainActivity
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.*
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.*
import timber.log.Timber
import java.time.LocalDateTime

class MiroConverter {
    data class HOLDERS(
        val B0_CLASS: String,
        val B0_ID: String,
        val LNAME: String,
        val FNAME: String,
    )

    data class CARDS(
        val B0_CLASS: String,
        val HOLDER_B0_ID: String,
        val CN: String
    )

    data class DELETE_HOLDERS(
        val B0_CLASS: String,
        val B0_ID: String
    )

    data class DELETE_CARDS(
        val B0_CLASS: String,
        val B0_ID: String,
    )

    data class DELETE_ACC_LEVELS_DISTR(
        val B0_CLASS: String,
        val B0_ID: String,
    )

    data class ACC_LEVELS_DISTR(
        val B0_CLASS: String,
        val HOLDER_B0_ID: String,
        val ACC_L_B0_ID: String,
        val EXPIRATION_DATE: String,
        val ACTIVATION_DATE: String
    )

    data class iftTermResponse(
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

    data class NewEventRequest( //TODO saljem serveru evente, nije jos miro slozio
        val ACT: String,
        val IFTTERM2_B0_ID: String,
        val CREAD: List<CREAD>
    )

    data class DEVS(
        @SerializedName("B0_ID") val B0_ID: String,
        @SerializedName("LOCAL_ID") val LOCAL_ID: String,
        @SerializedName("DESCR") val DESCR: String
    )

    data class EVENT_CODE2(
        @SerializedName("ID") val ID: String,
        @SerializedName("B0_DEV_E_CODE") val B0_DEV_E_CODE: String,
        @SerializedName("CLASESS") val CLASESS: String,
        @SerializedName("TITLE") val TITLE: String
    )

    data class initHcalObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("HOLDERS") val HOLDERS: ArrayList<HOLDERS>,
        @SerializedName("CARDS") val CARDS: ArrayList<CARDS>,
        @SerializedName("ACC_LEVELS_DISTR") val ACC_LEVELS_DISTR: ArrayList<ACC_LEVELS_DISTR>
    )

    data class deleteHcalObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("HOLDERS") val HOLDERS: ArrayList<DELETE_HOLDERS>,
        @SerializedName("CARDS") val CARDS: ArrayList<DELETE_CARDS>,
        @SerializedName("ACC_LEVELS_DISTR") val ACC_LEVELS_DISTR: ArrayList<DELETE_ACC_LEVELS_DISTR>
    )

    data class init0Object(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("IFTTERM2_B0_ID") val IFTTERM2_B0_ID: String,
        @SerializedName("IFTTERM2_DESCR") val IFTTERM2_DESCR: String,
        @SerializedName("DEVS") val DEVS: ArrayList<DEVS>,
        @SerializedName("EVENT_CODE2") val EVENT_CODE2: ArrayList<EVENT_CODE2>
    )

    data class Photo(
        @SerializedName("B0_CLASS") val B0_CLASS: String,
        @SerializedName("B0_ID") val B0_ID: String,
        @SerializedName("B0_HTTP_PATH") val B0_HTTP_PATH: String,
        @SerializedName("IMG_B64") val IMG_B64: String,
    )

    data class Holiday(
        @SerializedName("B0_ID") val B0_ID: String,
        @SerializedName("Y") val Y: String,
        @SerializedName("M") val M: String,
        @SerializedName("D") val D: String,
        @SerializedName("NO_WORK") val NO_WORK: Int,
        @SerializedName("DESCR") val DESCR: String,
    )

    data class PhotoObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("IFTTERM2_B0_ID") val IFTTERM2_B0_ID: String,
        @SerializedName("BASE64") val BASE64: String,
        @SerializedName("PHOTOS") val PHOTOS: ArrayList<Photo>
    )

    data class holidayObject(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("DAYS") val DAYS: ArrayList<Holiday>
    )

    suspend fun processRequest(operation: String): String {
        val scope = CoroutineScope(Dispatchers.IO)

        if (operation.contains("ADD_INIT1")) {
            val scope1 = CoroutineScope(Dispatchers.IO)
            val responseDeferred = scope1.async {
                val db = AppDatabase.getInstance(ContextProvider.getApplicationContext())
                db.clearAllTables()
            }
            responseDeferred.await()
        }

        when {
            (operation.contains("ADD_INIT1") || operation.contains("ADD_HCAL")) -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, initHcalObject::class.java)
                    Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
                    addHcal(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("IFTTERM2_INIT0") -> {
                //TODO not yet implemented
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, init0Object::class.java)
                    Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
                    init0(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
                return iftTermResponse(0, "1", "Not yet implemented").toString()
            }

            operation.contains("DELETE_HCAL") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, deleteHcalObject::class.java)
                    Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
                    deleteHcal(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("ADD_HOLIDAYS") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, holidayObject::class.java)
                    Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
                    addHoliday(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("PHOTOS") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, PhotoObject::class.java)
                    Timber.d("Msg: Got server request: ${objectic.ACT} | " + objectic.toString())
                    addPhotos(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            else -> {
                Timber.d("Msg: unknown request: %s", operation)
                return iftTermResponse(
                    iftTermResponse(
                        0,
                        "1",
                        "Unknown request: ${operation.substring(50)}"
                    )
                )
            }
        }
    }

    private fun addPhotos(objectic: PhotoObject): iftTermResponse {
        var counter = 0
        val personList = mutableListOf<Person>()

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))


            for (person in objectic.PHOTOS) {
                val p = db.PersonDao().get(person.B0_ID.toInt(), person.B0_CLASS)
                val newP = Person(
                    uid = p.uid,
                    classType = p.classType,
                    firstName = p.firstName,
                    lastName = p.lastName,
                    imageB64 = person.IMG_B64,
                    imagePath = person.B0_HTTP_PATH
                )
                personList.add(newP)
            }

            db.PersonDao().insertAll(personList)
            counter += personList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while putting photos in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        if ((personList.size == 0 && objectic.PHOTOS.size != 0)) {
            return iftTermResponse(
                counter = counter,
                err = "1",
                msg = "Not all entries were successfully added. Check log: ${LocalDateTime.now()}"
            )
        } else {
            return iftTermResponse(counter = counter, err = "0", msg = "Successful")
        }
    }

    private fun init0(objectic: init0Object): iftTermResponse {
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putString("IFTTERM2_B0_ID", objectic.IFTTERM2_B0_ID)
        editor.putString("IFTTERM2_DESCR", objectic.IFTTERM2_DESCR)

        return MiroConverter.iftTermResponse(0, "0", "msg")
    }

    suspend fun deleteHcal(objectic: deleteHcalObject): iftTermResponse {
        var deletionCounter = 0
        val cardList = mutableListOf<Card>()
        val accessLevelList = mutableListOf<AccessLevel>()
        val personList = mutableListOf<Person>()

        val scope1 = CoroutineScope(Dispatchers.IO)
        val responseDeferred1 = scope1.async {
            try {
                val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
                for (card in objectic.CARDS) {
                    cardList.add(db.CardDao().get(card.B0_ID.toInt(), card.B0_CLASS))
                }
                db.CardDao().deleteMany(cardList)
                deletionCounter += cardList.size
            } catch (e: Exception) {
                Timber.d(
                    "Exception while deleting cards in db: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
//                return@async iftTermResponse(
//                    counter = 0,
//                    err = "1",
//                    msg = "Exception while deleting cards in db: ${e.cause} | ${e.stackTraceToString()} | ${e.message}"
//                )
            }
        }
        responseDeferred1.await()
        print(responseDeferred1)

        val scope2 = CoroutineScope(Dispatchers.IO)
        val responseDeferred2 = scope2.async {
            try {
                val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
                for (ac in objectic.ACC_LEVELS_DISTR) {
                    accessLevelList.add(db.AccessLevelDao().get(ac.B0_ID.toInt()))
                }
                db.AccessLevelDao().deleteMany(accessLevelList)
                deletionCounter += accessLevelList.size
            } catch (e: Exception) {
                Timber.d(
                    "Exception while deleting access levels in db: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        }
        responseDeferred2.await()
        print(responseDeferred2)


        val scope3 = CoroutineScope(Dispatchers.IO)
        val responseDeferred3 = scope3.async {
            try {
                val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
                for (person in objectic.HOLDERS) {
                    personList.add(db.PersonDao().get(person.B0_ID.toInt(), person.B0_CLASS))
                }
                db.PersonDao().deleteMany(personList)
                deletionCounter += personList.size
            } catch (e: Exception) {
                Timber.d(
                    "Exception while deleting holders in db: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        }
        responseDeferred3.await()
        print(responseDeferred3)

        if ((cardList.size == 0 && objectic.CARDS.size != 0) || (accessLevelList.size == 0 && objectic.ACC_LEVELS_DISTR.size != 0) || (personList.size == 0 && objectic.HOLDERS.size != 0)) {
            return iftTermResponse(
                counter = deletionCounter,
                err = "1",
                msg = "Not all entries were successfully deleted. Check logs: ${LocalDateTime.now()}"
            )
        } else {
            return iftTermResponse(counter = deletionCounter, err = "0", msg = "Successful")
        }
    }

    fun addHcal(objectic: initHcalObject): iftTermResponse {
        var counter = 0

        val personList = mutableListOf<Person>()

        for (person in objectic.HOLDERS) {
            personList.add(
                Person(
                    uid = person.B0_ID.toInt(),
                    classType = person.B0_CLASS,
                    firstName = person.FNAME,
                    lastName = person.LNAME,
                    imageB64 = "",
                    imagePath = ""
                )
            )
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.PersonDao().insertAll(personList)
            counter += personList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while putting persons in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        val acList = mutableListOf<AccessLevel>()
        for (obj in objectic.ACC_LEVELS_DISTR) {
            acList.add(
                AccessLevel(
                    uid = obj.HOLDER_B0_ID.toInt(),
                    classType = obj.B0_CLASS,
                    accessLevel = obj.ACC_L_B0_ID.toInt()
                )
            )
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.AccessLevelDao().insertAll(acList)
            counter += acList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while deleting access lists in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        val cardList = mutableListOf<Card>()
        var i = 0

        for ((cards, ac) in objectic.CARDS.zip(objectic.ACC_LEVELS_DISTR)) {
            cardList.add(
                Card(
                    cardNumber = cards.CN,
                    classType = cards.B0_CLASS,
                    owner = cards.HOLDER_B0_ID.toInt(),
                    activationDate = ac.ACTIVATION_DATE,
                    expirationDate = ac.EXPIRATION_DATE,
                )
            )
            i++
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.CardDao().insertAll(cardList)
            counter += cardList.size
        } catch (e: Exception) {
            Timber.d("Exception while putting cards in db: %s | %s", e.message, e.cause)
        }
        if ((cardList.size == 0 && objectic.CARDS.size != 0) || (acList.size == 0 && objectic.ACC_LEVELS_DISTR.size != 0) || (personList.size == 0 && objectic.HOLDERS.size != 0)) {
            return iftTermResponse(
                counter = counter,
                err = "1",
                msg = "Not all entries were successfully added. Check log: ${LocalDateTime.now()}"
            )
        } else {
            return iftTermResponse(counter = counter, err = "0", msg = "Successful")
        }
    }

    private fun addHoliday(objectic: holidayObject): iftTermResponse {
        var counter = 0
        val calendarList = mutableListOf<Calendar>()
        for (c in objectic.DAYS) {
            calendarList.add(
                Calendar(
                    uid = c.B0_ID.toInt(),
                    day = c.D.toInt(),
                    month = c.M.toInt(),
                    year = c.Y.toInt(),
                    workDay = c.NO_WORK == 1,
                    description = c.DESCR
                )
            )
        }

        try {
            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
            db.CalendarDao().insertAll(calendarList)
            counter += calendarList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while putting calendar in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        if (calendarList.size == 0 && objectic.DAYS.size != 0) {
            return iftTermResponse(
                counter = counter,
                err = "1",
                msg = "Not all entries were successfully added. Check log: ${LocalDateTime.now()}"
            )
        } else {
            return iftTermResponse(counter = counter, err = "0", msg = "Successful")
        }
    }

    fun iftTermResponse(response: iftTermResponse): String {
        return "{\"ACT\": \"IFTSRV2_RESPONSE\",\"NUM_CREAD\": \"${response.counter}\",\"ERROR\": {\"CODE\": \"${response.err}\",\"TEXT\": \"${response.msg}\"}}"
    }

    fun pushEventFormat(cardResponse: Bundle): String {
        var eCode = 0 //TODO ECODE
        when (cardResponse.get("selection")) {
            "BE-TO" -> eCode = 0
            "Liječnik" -> eCode = 3
            "Privatno" -> eCode = 4
            "Pauza" -> eCode = 5
            "Poslovno" -> eCode = 6
        }

        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val id = prefs.getInt("IFTTERM2_B0_ID", 0)

//        var strNew = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"${id}\",\"CREAD\":["
//        strNew += "{\"CN\":\"${cardResponse.get("CardCode")}\", \"GENT\":\"${cardResponse.get("DateTime")}\", \"ECODE\":\"0\", \"DEV_B0_ID\":\"0\"}]}"

        return "{\"ACT\": \"NEW_EVENTS\",\"IFTTERM2_B0_ID\": \"${id}\",\"CREAD\": [{\"CN\": \"${
            cardResponse.get(
                "CardCode"
            )
        }\",\"GENT\": \"${cardResponse.get("DateTime")}\",\"ECODE\": \"1\",\"ECODE2\": \"1\",\"DEV_B0_ID\": \"0\"}]}"
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
                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\": \"1\",\"ECODE2\": \"1\", \"DEV_B0_ID\":\"0\"},"
                } else {
                    strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\": \"1\",\"ECODE2\": \"1\", \"DEV_B0_ID\":\"0\"}]}"
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