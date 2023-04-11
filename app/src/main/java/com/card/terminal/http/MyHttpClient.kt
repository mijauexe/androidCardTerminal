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
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import timber.log.Timber
import java.net.ConnectException
import java.nio.ByteBuffer
import java.util.*

object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null

    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope

    private var larusCheckScansTask: TimerTask? = null

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

        database = AppDatabase.getInstance((ContextProvider.getApplicationContext()))

        startNettyServer()

        (larusCheckScansTask as LarusCheckScansTask).startTask()

//        Handler().postDelayed({
//            stopLarusSocket()
//        }, 10000)

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

    fun pingy(bundle: Bundle) {
//        larusFunctions?.setDoorTime(15000, 15000, 1000, 1000)
        larusFunctions?.openDoor(1)
        publishNewEvent(bundle)
    }

    suspend fun getSocketResponse(
        sendChannel: ByteWriteChannel,
        receiveChannel: ByteReadChannel,
        buffer: ByteBuffer
    ): ByteArray {
        sendChannel.writeAvailable(buffer)

        var i = 0
        var byteResponseArray = byteArrayOf()

        while (true) {
            try {
                val response = receiveChannel.readByte()
                byteResponseArray += response
            } catch (e: Exception) {
                break
            }
            i += 1
        }
        return byteResponseArray
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
//            server.stopServerOnCancellation()
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopLarusSocket()
        Timber.d("Msg: MyHttpClient stopped")
    }

    fun isClientReady(): Boolean {
        return client != null
    }

    fun publishNewEvent(cardResponse: Bundle) {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            eventToDatabase(cardResponse, false)
            val body = MiroConverter().convertToNewEventFormat(cardResponse)
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
                            client?.post("gas") {
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

    suspend fun publishUnpublishedEvents() {
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
        val esp = MiroConverter().getFormattedUnpublishedEvents()

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
                                setBody(esp.eventString)
                            }
                        }
                if (response != null) {
                    if (response.bodyAsText().contains("\"CODE\":\"0\"") && response.bodyAsText()
                            .contains("\"NUM_CREAD\":\"${esp.eventList.size}\"")
                    ) {
                        updateEvents(esp.eventList)
                        Timber.d(
                            "Msg: Event list updated and published: %s", esp.eventList
                        )
                    }
                }
            } catch (ce: ConnectException) {
                Timber.d("Msg: Event list not updated or published: %s", esp.eventList)
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

    fun eventToDatabase(cardResponse: Bundle, published: Boolean) {
        val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
        if (published) {
            val e = db.EventDao()
                .getLastScanEventWithCardNumber(Integer.valueOf(cardResponse.get("CardCode") as String))
            val newE = Event(
                uid = e.uid,
                eventCode = e.eventCode,
                cardNumber = e.cardNumber,
                dateTime = e.dateTime,
                published = true
            )
            db.EventDao().update(newE)
        } else {
            val event = Event(
                eventCode = MiroConverter().convertECode(
                    cardResponse.get("selection").toString()
                ),
                cardNumber = cardResponse.get("CardCode").toString().toInt(),
                dateTime = cardResponse.get("DateTime").toString(),
                published = published,
                uid = 0 //auto-generate
            )
            db.EventDao().insert(event)
        }
    }

    fun updateEvents(list: List<Event>) {
        val newEvents = mutableListOf<Event>()
        for (e in list) {
            newEvents.add(
                Event(
                    uid = e.uid,
                    eventCode = e.eventCode,
                    cardNumber = e.cardNumber,
                    dateTime = e.dateTime,
                    published = true
                )
            )
        }
        val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
        db.EventDao().updateEvents(newEvents)
    }
}