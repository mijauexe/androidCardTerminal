package com.card.terminal.utils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.card.terminal.BuildConfig
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
        @SerializedName("B0_CLASS") val B0_CLASS: String,
        @SerializedName("B0_ID") val B0_ID: String,
        @SerializedName("LNAME") val LNAME: String,
        @SerializedName("FNAME") val FNAME: String,
        @SerializedName("PHOTO_B0_HTTP_PATH") val PHOTO_B0_HTTP_PATH: String,
        @SerializedName("COMP_SHORT_NAME") val COMP_SHORT_NAME: String,
    )

    data class CARDS(
        @SerializedName("B0_CLASS") val B0_CLASS: String,
        @SerializedName("HOLDER_B0_ID") val HOLDER_B0_ID: String,
        @SerializedName("CN") val CN: String,
        @SerializedName("ACTIVATION_DATE") val ACTIVATION_DATE: String,
        @SerializedName("EXPIRATION_DATE") val EXPIRATION_DATE: String
    )

    data class DELETE_HOLDERS(
        val B0_CLASS: String, val B0_ID: String
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
        val counter: Int, val err: String, val msg: String
    )

    data class EventStringPair(
        val eventList: List<Event>, val eventString: String
    )

    data class DEVS(
        @SerializedName("B0_ID") val B0_ID: String,
        @SerializedName("LOCAL_ID") val LOCAL_ID: String,
        @SerializedName("CONTROL_IN") val CONTROL_IN: String,
        @SerializedName("CONTROL_OUT") val CONTROL_OUT: String,
        @SerializedName("DESCR") val DESCR: String
    )

    data class EVENT_CODE2(
        @SerializedName("ID") val ID: String,
        @SerializedName("B0_DEV_E_CODE") val B0_DEV_E_CODE: String,
        @SerializedName("CLASESS") val CLASESS: String,
        @SerializedName("LABEL") val LABEL: String,
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
//        @SerializedName("CARDS") val CARDS: ArrayList<DELETE_CARDS>,
        @SerializedName("CARDS") val CARDS: ArrayList<String>,
        @SerializedName("ACC_LEVELS_DISTR") val ACC_LEVELS_DISTR: ArrayList<DELETE_ACC_LEVELS_DISTR>
    )

    data class B0_SERVER(
        @SerializedName("IP") val IP: String,
        @SerializedName("HTTP_PORT") val HTTP_PORT: String,
    )

    data class DEFINITIONS(
        @SerializedName("ID") val ID: String, @SerializedName("DESCR") val DESCR: String
    )

    data class SCHEDULE(
        @SerializedName("DAY_DESCR") val DAY_DESCR: String,
        @SerializedName("MODE_ID") val MODE_ID: String,
        @SerializedName("TIME_FROM") val TIME_FROM: String,
        @SerializedName("TIME_TO") val TIME_TO: String
    )

    data class OPERATION_MODE(
        @SerializedName("DEFINITIONS") val DEFINITIONS: ArrayList<DEFINITIONS>,
        @SerializedName("SCHEDULE") val SCHEDULE: ArrayList<SCHEDULE>
    )

    data class IMAGES(
        @SerializedName("CAPTURE_ON_EVENT") val CAPTURE_ON_EVENT: String,
        @SerializedName("IMG_SIZE") val IMG_SIZE: String,
        @SerializedName("SEND_TO_IFTSRV2") val SEND_TO_IFTSRV2: String,
    )


    data class init0Object(
        @SerializedName("ACT") val ACT: String,
        @SerializedName("IFTTERM2_B0_ID") val IFTTERM2_B0_ID: String,
        @SerializedName("IFTTERM2_DESCR") val IFTTERM2_DESCR: String,
        @SerializedName("OPERATION_MODE") val OPERATION_MODE: OPERATION_MODE,
        @SerializedName("IMAGES") val IMAGES: IMAGES,
        @SerializedName("B0_SERVER") val B0_SERVER: B0_SERVER,
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
        @SerializedName("ACT") val ACT: String, @SerializedName("DAYS") val DAYS: ArrayList<Holiday>
    )

    suspend fun processRequest(operation: String): String {
        val scope = CoroutineScope(Dispatchers.IO)
        Timber.d("Got server request: $operation")
        if (operation.contains("ADD_INIT1")) {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.CardDao().deleteAll()
            db.AccessLevelDao().deleteAll()
            db.PersonDao().deleteAll()
        }

        when {
            (operation.contains("ADD_HCAL") || operation.contains("ADD_INIT1")) -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, initHcalObject::class.java)
                    addHcal(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("IFTTERM2_INIT0") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, init0Object::class.java)
                    init0(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("DELETE_HCAL") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, deleteHcalObject::class.java)
                    deleteHcal(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("ADD_HOLIDAYS") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, holidayObject::class.java)
                    addHoliday(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            operation.contains("PHOTOS") -> {
                val responseDeferred = scope.async {
                    val objectic = Gson().fromJson(operation, PhotoObject::class.java)
                    addPhotos(objectic)
                }
                val response = responseDeferred.await()
                return iftTermResponse(response)
            }

            else -> {
                Timber.d("Msg: unknown request: %s", operation)
                return iftTermResponse(
                    iftTermResponse(
                        0, "1", "Unknown request: ${operation.substring(50)}"
                    )
                )
            }
        }
    }

    private fun addPhotos(objectic: PhotoObject): iftTermResponse {
        var counter = 0
        val personList = mutableListOf<Person>()

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )

            for (person in objectic.PHOTOS) {
                try {
                    val p = db.PersonDao().get(person.B0_ID.toInt(), person.B0_CLASS)
                    val newP = p?.let {
                        Person(
                            uid = it.uid,
                            classType = p.classType,
                            firstName = p.firstName,
                            lastName = p.lastName,
                            imageB64 = person.IMG_B64,
                            imagePath = person.B0_HTTP_PATH,
                            companyName = p.companyName
                        )
                    }
                    if (newP != null) {
                        personList.add(newP)
                    }
                } catch (e: java.lang.Exception) {
                    Timber.d(
                        "Msg: Exception %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                } catch (e: Exception) {
                    Timber.d(
                        "Msg: Exception %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                }
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

    private suspend fun init0(objectic: init0Object): iftTermResponse {
        var counter = 0
        //REMOVE THIS SHIT, samo dukat tablet ima ovo
//        return iftTermResponse(counter = counter, err = "0", msg = "Maintenance, request ignored")

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.OperationModeDao().deleteAll()
        } catch (e: Exception) {
            Timber.d(
                "Exception while deleting operations in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.OperationScheduleDao().deleteAll()
        } catch (e: Exception) {
            Timber.d(
                "Exception while deleting schedules in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.DeviceDao().deleteAll()
        } catch (e: Exception) {
            Timber.d(
                "Exception while deleting devices in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        withContext(Dispatchers.Main) {
            val prefs = ContextProvider.getApplicationContext()
                .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putInt("IFTTERM2_B0_ID", objectic.IFTTERM2_B0_ID.toInt())
            editor.putInt(
                "DEV_B0_ID", objectic.DEVS[0].B0_ID.toInt()
            ) //TODO OVDJE UZIMAM SAMO PRVI JER POSTOJI SAMO 1 UREDAJ

            if (objectic.IMAGES.CAPTURE_ON_EVENT.equals("YES")) {
                editor.putBoolean("CaptureOnEvent", true)
            } else {
                editor.putBoolean("CaptureOnEvent", false)
            }

            if (objectic.IMAGES.IMG_SIZE.equals("SMALL")) {
                editor.putInt("ImageSize", 50)
            } else if (objectic.IMAGES.IMG_SIZE.equals("MEDIUM")) {
                editor.putInt("ImageSize", 75)
            } else {
                editor.putInt("ImageSize", 100)
            }

            if (objectic.IMAGES.SEND_TO_IFTSRV2.equals("YES")) {
                editor.putBoolean("pushImageToServer", true)
            } else {
                editor.putBoolean("pushImageToServer", false)
            }

            editor.putString("IFTTERM2_DESCR", objectic.IFTTERM2_DESCR)
            editor.putString(
                "serverIP", "http://" + objectic.B0_SERVER.IP + "/b0pass/b0pass_iftp2.php"
            )
            editor.putInt("serverPort", objectic.B0_SERVER.HTTP_PORT.toInt())
            counter += 4
            editor.apply()
        }

        val operationModeList = mutableListOf<OperationMode>()

        for (def in objectic.OPERATION_MODE.DEFINITIONS) {
            operationModeList.add(OperationMode(uid = def.ID.toInt(), description = def.DESCR))
        }

        val operationScheduleList = mutableListOf<OperationSchedule>()

        for (sch in objectic.OPERATION_MODE.SCHEDULE) {
            operationScheduleList.add(
                OperationSchedule(
                    uid = 0, //auto-generate
                    description = sch.DAY_DESCR,
                    timeFrom = sch.TIME_FROM,
                    timeTo = sch.TIME_TO,
                    modeId = sch.MODE_ID.toInt()
                )
            )
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.OperationModeDao().insertAll(operationModeList)
            counter += operationModeList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while adding operations in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.OperationScheduleDao().insertAll(operationScheduleList)
            counter += operationScheduleList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while adding operations in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.DeviceDao().insert(
                Device(
                    uid = objectic.DEVS[0].B0_ID.toInt(),
                    localId = objectic.DEVS[0].LOCAL_ID.toInt(),
                    controlIn = 0/*objectic.DEVS[0].CONTROL_IN.toInt()*/,
                    controlOut = 0/*objectic.DEVS[0].CONTROL_OUT.toInt()*/,
                    description = objectic.DEVS[0].DESCR
                )
            )
            counter += 1
        } catch (e: Exception) {
            Timber.d(
                "Exception while adding device in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }

        if (BuildConfig.RelayAlarm) {
            AlarmUtils().rescheduleAlarms()
        }

        parseButtons(objectic.EVENT_CODE2)

        if ((objectic.DEVS.size != 0 && counter == 0)) {
            return iftTermResponse(
                counter = 0,
                err = "1",
                msg = "Not all devices were successfully added. Check logs: ${LocalDateTime.now()}"
            )
        } else {
            return iftTermResponse(counter = counter, err = "0", msg = "Successful")
        }
    }

    private suspend fun parseButtons(eventCode2: ArrayList<EVENT_CODE2>) {
        val mut = mutableListOf<Button>()
        val classesSet = mutableSetOf<String>()
        var counter = 0

        for (btn in eventCode2) {
            val classes = btn.CLASESS

            if(classes.contains(",")) {
                val multipleClasses = classes.split(",")
                val trimmedArray = multipleClasses.map { it.trim() }
                classesSet.addAll(trimmedArray)

                for (i in trimmedArray) {
                    mut.add(
                        Button(
                            uid = 0,
                            classType = i,
                            label = btn.LABEL,
                            title = btn.TITLE,
                            eCode2 = btn.ID.toInt(),
                            eCode = btn.B0_DEV_E_CODE.toInt()
                        )
                    )
                }

            } else {
                classesSet.add(btn.CLASESS)
                mut.add(
                    Button(
                        uid = 0,
                        classType = btn.CLASESS,
                        label = btn.LABEL,
                        title = btn.TITLE,
                        eCode2 = btn.ID.toInt(),
                        eCode = btn.B0_DEV_E_CODE.toInt()
                    )
                )
            }
        }

        if (classesSet.size > 0) {
            withContext(Dispatchers.Main) {
                val prefs = ContextProvider.getApplicationContext()
                    .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
                val editor = prefs.edit()
                editor.putStringSet("classes", classesSet)
                editor.commit()
            }
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.ButtonDao().deleteAll()
            db.ButtonDao().insertAll(mut)
            counter += mut.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while adding buttons in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
        }
    }

    suspend fun deleteHcal(objectic: deleteHcalObject): iftTermResponse {
        var deletionCounter = 0
        val cardList = mutableListOf<String>()
        val accessLevelList = mutableListOf<AccessLevel>()
        val personList = mutableListOf<Person>()

        val scope1 = CoroutineScope(Dispatchers.IO)
        val responseDeferred1 = scope1.async {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )

            for (card in objectic.CARDS) {
                try {
                    db.CardDao().deleteByCardNumber(card.toInt())?.let { cardList.add(card) }
                    deletionCounter += 1
                } catch (e: Exception) {
                    Timber.d(
                        "Exception while deleting cards in db: %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                }
            }
        }
        responseDeferred1.await()
        print(responseDeferred1)

        val scope2 = CoroutineScope(Dispatchers.IO)
        val responseDeferred2 = scope2.async {
            try {
                val db = AppDatabase.getInstance(
                    ContextProvider.getApplicationContext(),
                    Thread.currentThread().stackTrace
                )

                for (ac in objectic.ACC_LEVELS_DISTR) {
                    db.AccessLevelDao().get(ac.B0_ID.toInt())?.let { accessLevelList.add(it) }
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
                val db = AppDatabase.getInstance(
                    ContextProvider.getApplicationContext(),
                    Thread.currentThread().stackTrace
                )

                for (person in objectic.HOLDERS) {
                    db.PersonDao().get(person.B0_ID.toInt(), person.B0_CLASS)
                        ?.let { personList.add(it) }
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

    suspend fun addHcal(objectic: initHcalObject): iftTermResponse {
        var counter = 0

        val personList = mutableListOf<Person>()
        val classesSet = mutableSetOf<String>()

        for (person in objectic.HOLDERS) {
            classesSet.add(person.B0_CLASS)
            personList.add(
                Person(
                    uid = person.B0_ID.toInt(),
                    classType = person.B0_CLASS,
                    firstName = person.FNAME,
                    lastName = person.LNAME,
                    imageB64 = "",
                    imagePath = person.PHOTO_B0_HTTP_PATH,
                    companyName = person.COMP_SHORT_NAME
                )
            )
        }

        //ako za pojedinu klasu ne postoji tipka
        withContext(Dispatchers.Main) {
            val prefs = ContextProvider.getApplicationContext()
                .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
            val editor = prefs.edit()

            //remove old button crap
            try {
                for ((key, value) in prefs.all) {
                    if (key.contains("noButtons")) {
                        when (value) {
                            is String -> {}

                            is Int -> {}

                            is Boolean -> { editor.remove(key) }

                            else -> {}
                        }
                    }
                }
                editor.commit()
            } catch (e: Exception) {
                Timber.d(e.stackTraceToString())
            }

            val init0Classes: MutableSet<String> = prefs.getStringSet("classes", mutableSetOf<String>())!!

            if(init0Classes.size > 0) {
                val difference = classesSet.subtract(init0Classes)
                if (difference.isNotEmpty()) {
                    //ove klase nemaju tipke
                    for (diff in difference) {
                        editor.putBoolean("${diff}_noButtons", true)
                        editor.commit()
                    }
                }
            }
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
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
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
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

        for (cards in objectic.CARDS) {
            cardList.add(
                Card(
                    cardNumber = cards.CN.toInt(),
                    classType = cards.B0_CLASS,
                    owner = cards.HOLDER_B0_ID.toInt(),
                    activationDate = cards.ACTIVATION_DATE,
                    expirationDate = cards.EXPIRATION_DATE
                )
            )
            i++
        }

        try {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.CardDao().insertAll(cardList)

//            for (card in cardList) {
//                try {
//                    db.CardDao().deleteByCardNumber(card.cardNumber)
//                } catch (e: java.lang.Exception) {
//                    Timber.d(
//                        "Msg: Exception %s | %s | %s",
//                        e.cause,
//                        e.stackTraceToString(),
//                        e.message
//                    )
//                }
//                db.CardDao().insert(card)
//            }

            counter += cardList.size
        } catch (e: Exception) {
            Timber.d(
                "Exception while putting cards in db: %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
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
        val calendarList = mutableListOf<com.card.terminal.db.entity.Calendar>()
        for (c in objectic.DAYS) {
            calendarList.add(
                com.card.terminal.db.entity.Calendar(
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
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
            )
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
        val eCode = cardResponse.getInt("eCode")
        var eCode2 = cardResponse.getInt("eCode2", 0)

        //TODO zasad je samo jedan uredaj, pa cu dohvatit onaj na indeksu 0 zbog uid-a
//        val deviceList = mutableListOf<Device>()
//        try {
//            val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
//            deviceList.addAll(db.DeviceDao().getAll())
//        } catch (e: Exception) {
//            Timber.d(
//                "Exception while getting device in db: %s | %s | %s",
//                e.cause,
//                e.stackTraceToString(),
//                e.message
//            )
//        }

        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
        val id = prefs.getInt("IFTTERM2_B0_ID", 666)
        val dev_b0_id = prefs.getInt("DEV_B0_ID", 666)

        if (cardResponse.getBoolean("NoOptionPressed")) {
            eCode2 = 0
        }

        var img = cardResponse.getString("EventImage", "")
        if (!prefs.getBoolean("pushImageToServer", false)) {
            img = ""
        }

        val rtr2 = "{\n" +
                "    \"ACT\": \"NEW_EVENTS\",\n" +
                "    \"IFTTERM2_B0_ID\": \"${id}\",\n" +
                "    \"CREAD\": [\n" +
                "        {\n" +
                "            \"CN\": \"${cardResponse.getString("CardCode", "")}\",\n" +
                "            \"GENT\": \"${cardResponse.getString("DateTime", "")}\",\n" +
                "            \"ECODE\": \"${eCode}\",\n" +
                "            \"ECODE2\": \"${eCode2}\",\n" +
                "            \"DEV_B0_ID\": \"${dev_b0_id}\",\n" +
                "            \"IMG_B64\": \"${img}\"\n" +
                "        }\n" +
                "    ]\n" +
                "}"

        return rtr2
    }

    suspend fun getFormattedUnpublishedEvents(iftTermId: Int): EventStringPair {
        var strNew = "{\"ACT\": \"NEW_EVENTS\",  \"IFTTERM2_B0_ID\":\"${iftTermId}\",\"CREAD\":["
        val unpublishedEvents = mutableListOf<Event>()
        val scope = CoroutineScope(Dispatchers.IO)
        //TODO ECODE
        val responseDeferred = scope.async {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            db.EventDao().getUnpublishedEvents()?.let { unpublishedEvents.addAll(it) }
            var dev_b0_id = 0
            try {
                dev_b0_id = db.DeviceDao().getAll()
                    ?.get(0)?.uid!!  //TODO dev_b0_id pitaj miru kaj s tim, zasad je samo 1 uredaj, ali to se mora spremat u bazu skupa s eventom, koji uredaj je izgenerirao -> njegov b0 id mi treba ovdje, ovo je retardirano

            } catch (e: java.lang.IndexOutOfBoundsException) {
                //ako nema uredaja, samo salji 0
            } catch (e: Exception) {
                //ako nema uredaja, samo salji 0

            } catch (e: java.lang.Exception) {
                //ako nema uredaja, samo salji 0
            }

            if (unpublishedEvents.size != 0) {
                for (ue in unpublishedEvents.indices) {
                    if (ue != unpublishedEvents.size - 1) {
                        strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\": \"${unpublishedEvents[ue].eventCode}\",\"ECODE2\": \"${unpublishedEvents[ue].eventCode2}\", \"DEV_B0_ID\":\"${dev_b0_id}\", \"IMG_B64\":\"${unpublishedEvents[ue].image}\"},"
                    } else {
                        strNew += "{\"CN\":\"${unpublishedEvents[ue].cardNumber}\", \"GENT\":\"${unpublishedEvents[ue].dateTime}\", \"ECODE\": \"${unpublishedEvents[ue].eventCode}\",\"ECODE2\": \"${unpublishedEvents[ue].eventCode2}\", \"DEV_B0_ID\":\"${dev_b0_id}\", \"IMG_B64\":\"${unpublishedEvents[ue].image}\"}]}"
                    }
                }
            }

        }
        responseDeferred.await()
        return EventStringPair(unpublishedEvents, strNew)
    }

}