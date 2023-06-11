package com.card.terminal.http

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Event
import com.card.terminal.http.plugins.configureRouting
import com.card.terminal.http.plugins.configureSerialization
import com.card.terminal.http.tasks.LarusCheckScansTask
import com.card.terminal.http.tasks.PublishEventsTask
import com.card.terminal.main
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.MiroConverter
import com.card.terminal.utils.larusUtils.LarusFunctions
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.util.*

object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null

    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope

    private var larusCheckScansTask: TimerTask? = null

    private var publishEventsTask: TimerTask? = null

    private var larusFunctions: LarusFunctions? = null

    lateinit var server: NettyApplicationEngine

    fun bindHttpClient(code: MutableLiveData<Map<String, String>>) {
        client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
            install(HttpRequestRetry) {
                retryOnServerErrors(maxRetries = 3)
                exponentialDelay()
                retryIf { request, response ->
                    !response.status.isSuccess()
                }
                retryOnExceptionIf { request, cause ->
                    cause is Exception
                }
                delayMillis { retry ->
                    retry * 3000L
                } // retries in 3, 6, 9, etc. seconds
            }
        }

        mutableCode = code

        larusFunctions = LarusFunctions(client!!, mutableCode)
        larusCheckScansTask = LarusCheckScansTask(larusFunctions!!)
        publishEventsTask = PublishEventsTask()

        database = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(),
            Thread.currentThread().stackTrace
        )

        startNettyServer()

        (larusCheckScansTask as LarusCheckScansTask).startTask()
        (publishEventsTask as PublishEventsTask).startTask()

        larusFunctions?.changeRelayMode(1, 0)
        larusFunctions?.changeRelayMode(2, ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getInt("relay2State", 0))
    }

    fun stopLarusSocket() {
        (larusCheckScansTask as LarusCheckScansTask).stopTask()
    }

    fun startLarusSocket() {
        if (!((larusCheckScansTask as LarusCheckScansTask).started)) {
            larusCheckScansTask = LarusCheckScansTask(larusFunctions!!)
            (larusCheckScansTask as LarusCheckScansTask).startTask()
        }
    }

    fun openDoor(doorNum: Int) {
//        larusFunctions?.setDoorTime(3000, 5000, 0, 0)
        larusFunctions?.openDoor(doorNum)
    }

    fun checkDoor(doorNum: Int) {
        larusFunctions?.stateDoor(doorNum)
    }

    fun hepPort1RelaysToggle(noButtonClickNeededRegime: Boolean) {
        if (noButtonClickNeededRegime) {
            larusFunctions?.changeRelayMode(2, 1) //1 je hold
        } else {
            larusFunctions?.changeRelayMode(2, 0) //0 je pulse
        }
    }

    fun relayMode(doorNum: Int, pulseOrHold: Int) {
        //0 - vrata su normalnom modu rada  - prolaz s karticama, 1 - vrata nisu kontrolirana
        larusFunctions?.changeRelayMode(doorNum, pulseOrHold)
    }

    fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        var ctr = 0
        for (b in byteArray) {
            result += Math.pow(2.0, ctr.toDouble()).toInt() * b.toUByte().toInt()
            ctr += 8
        }
        return result
    }

    fun startNettyServer() {
        stop()
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            database?.let { main(it) }
            server = embeddedServer(Netty, port = 6969) {
                configureSerialization()
                configureRouting()
            }
            Runtime.getRuntime().addShutdownHook(Thread {
                server.stop(1_000, 2_000)
                Timber.d("Msg: Netty server stopped")
            })
            server.start(wait = true)
            Timber.d("Msg: Netty server started")
        }
    }

    fun stop() {
        if (::server.isInitialized) {
            server.stop(1_000, 2_000)
        }
        try {
            if (this::scope.isInitialized && scope.isActive) {
                scope.cancel()
                Timber.d("Msg: MyHttpClient stopped")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopLarusSocket()
    }

    fun isClientReady(): Boolean {
        return client != null
    }

    fun pingEndpoint() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
//            val larusEndpoint = getPortAndIP()
            val response: HttpResponse =
                client!!.request("192.168.88.219/b0pass/b0pass_iftp2.php") {
                    method = HttpMethod.Get
                }
            println(response)
//            mutableCode.postValue(mapOf("pingResponse" to response.toString()))
        }
    }

    fun pushRequest(type: String) {
        val scope1 = CoroutineScope(Dispatchers.IO)
        scope1.launch {
            val mySharedPreferences =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)

            try {
                val response =
                    mySharedPreferences.getString(
                        "serverIP",
                        ""
                    )
                        ?.let {
                            client?.post(it) {
                                contentType(ContentType.Application.Json)
                                if (type.contains("PHOTOS")) {
                                    setBody(
                                        "{ \n" +
                                                "    \"ACT\": \"${type}\", \n" +
                                                "    \"IFTTERM2_B0_ID\": \"${
                                                    mySharedPreferences.getInt(
                                                        "IFTTERM2_B0_ID",
                                                        4
                                                    )
                                                }\",\n" +
                                                "    \"BASE64\":\"OFF\"\n" +
                                                "}"
                                    )
                                } else {
                                    setBody(
                                        "{\"ACT\": \"${type}\",\"IFTTERM2_B0_ID\": \"${
                                            mySharedPreferences.getInt(
                                                "IFTTERM2_B0_ID",
                                                4
                                            )
                                        }\"}"
                                    )
                                }
                            }
                        }
                if (response != null) {
                    Timber.d("Msg: Requested ${type}, got ${response.bodyAsText(Charsets.UTF_8)}")
                    MiroConverter().processRequest(response.bodyAsText())
                }
            } catch (e: NoRouteToHostException) {
                Timber.d(
                    "Msg: No route to host: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            } catch (ce: ConnectException) {
                Timber.d(
                    "Msg: Destination cannot be reached: %s | %s | %s",
                    ce.cause,
                    ce.stackTraceToString(),
                    ce.message
                )
            } catch (e: Exception) {
                Timber.d(
                    "Exception: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        }
    }

    fun publishNewEvent(cardResponse: Bundle) {
        val scope1 = CoroutineScope(Dispatchers.IO)
        scope1.launch {
            eventToDatabase(cardResponse, false)
            val body = withContext(Dispatchers.Main) {
                MiroConverter().pushEventFormat(cardResponse)
            }
            val mySharedPreferences =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            try {
                val response =
                    mySharedPreferences.getString(
                        "serverIP",
                        ""
                    )
                        ?.let {
                            client?.post(it) {
                                contentType(ContentType.Application.Json)
                                setBody(body)
                            }
                        }
                if (response != null) {
                    println(response.bodyAsText())
                    if (response.bodyAsText().contains("\"CODE\":\"0\"")
                        && response.bodyAsText()
                            .contains("\"NUM_CREAD\":\"1\"")
                    )
                        eventToDatabase(cardResponse, true)
                    Timber.d(
                        "Msg: user %s scanned, response sent to server: %b",
                        cardResponse,
                        true
                    )
                }
            } catch (ce: ConnectException) {
                Timber.d("Msg: user %s scanned, response sent to server: %b", cardResponse, false)
            } catch (e: Exception) {
                Timber.d(
                    "Exception while publishing event(s) to server: %s | %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message,
                    body
                )
            }
        }
    }

    fun publishUnpublishedEvents() {
        val scopeSven = CoroutineScope(Dispatchers.IO)
        scopeSven.launch {

            val mySharedPreferences = withContext(Dispatchers.Main) {
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            }

            val esp = MiroConverter().getFormattedUnpublishedEvents(
                mySharedPreferences.getInt(
                    "IFTTERM2_B0_ID",
                    696969
                )
            )

            if (esp.eventList.isNotEmpty()) {
                try {
                    val response =
                        mySharedPreferences.getString(
                            "serverIP",
                            ""
                        )
                            ?.let {
                                client?.post(it) {
                                    contentType(ContentType.Application.Json)
                                    setBody(esp.eventString)
                                }
                            }
                    if (response != null) {
                        if (response.bodyAsText()
                                .contains("\"CODE\":\"0\"") && response.bodyAsText()
                                .contains("\"NUM_CREAD\":\"${esp.eventList.size}\"")
                        ) {
                            updateEvents(esp.eventList)
                            Timber.d(
                                "Msg: Event list updated and published: %s", esp.eventString
                            )
                        }
                    }
                } catch (ce: ConnectException) {
                    Timber.d("Msg: Event list not updated or published: %s", esp.eventString)
                } catch (e: Exception) {
                    Timber.d(
                        "Exception while publishing unpublished event(s) to server: %s | %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message,
                        esp.eventString
                    )
                }
            }
        }
    }

    fun eventToDatabase(cardResponse: Bundle, published: Boolean) {
        val scope1 = CoroutineScope(Dispatchers.IO)
        scope1.launch {

            val mySharedPreferences = withContext(Dispatchers.Main) {
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            }


            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(),
                Thread.currentThread().stackTrace
            )
            if (published) {
                val e = db.EventDao()
                    .getLastScanEventWithCardNumber(Integer.valueOf(cardResponse.get("CardCode") as String))
                val newE = e?.let {
                    Event(
                        uid = it.uid,
                        eventCode = it.eventCode,
                        eventCode2 = it.eventCode2,
                        cardNumber = it.cardNumber,
                        dateTime = it.dateTime,
                        published = true,
                        deviceId = it.deviceId
                    )
                }
                if (newE != null) {
                    db.EventDao().update(newE)
                }
            } else {
                val event = Event(
                    eventCode = 2, //TODO
                    eventCode2 = cardResponse.getInt("eCode2"),
                    cardNumber = cardResponse.get("CardCode").toString().toInt(),
                    dateTime = cardResponse.get("DateTime").toString(),
                    published = published,
                    uid = 0, //auto-generate
                    deviceId = mySharedPreferences.getInt("IFTTERM2_B0_ID", 696969) //TODO
                )
                db.EventDao().insert(event)
            }


        }


    }

    fun updateEvents(list: List<Event>) {
        val newEvents = mutableListOf<Event>()
        for (e in list) {
            newEvents.add(
                Event(
                    uid = e.uid,
                    eventCode = e.eventCode,
                    eventCode2 = e.eventCode2,
                    cardNumber = e.cardNumber,
                    dateTime = e.dateTime,
                    published = true,
                    deviceId = 0 //TODO
                )
            )
        }
        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(),
            Thread.currentThread().stackTrace
        )
        db.EventDao().updateEvents(newEvents)
    }
}