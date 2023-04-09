package com.card.terminal.http

import android.app.Application
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.View
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
        posaljiOcitanje(bundle)
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
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            database?.let { main(it) }
            val server = embeddedServer(Netty, port = 6969) {
                configureSerialization()
                configureRouting()
            }

            Runtime.getRuntime().addShutdownHook(Thread {
                server.stop(0, 0)
                Timber.d("Msg: Netty server stopped")
            })
            server.start(wait = true)
            Timber.d("Msg: Netty server started")
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
        stopLarusSocket()
        Timber.d("Msg: MyHttpClient stopped")
    }

    fun isClientReady(): Boolean {
        return client != null
    }

    fun posaljiOcitanje(cardResponse: Bundle) {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {

            val mySharedPreferences =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)

            try {
                val response =
                    mySharedPreferences.getString(
                        "serverIP",
                        "http://sucic.info/b0pass/b0pass_iftp2.php"
                    )
                        ?.let {
                            client?.post(it) {
                                contentType(ContentType.Application.Json)
                                setBody(MiroConverter().convertToPOSTFormat(cardResponse))
                            }
                        }
                if (response != null) {
                    println(response.bodyAsText())
                }
                insertInDatabase(cardResponse, true)
                Timber.d("Msg: user %s scanned, response sent to server: %b", cardResponse, true)
            } catch (ce: ConnectException) {
                insertInDatabase(cardResponse, false)
                Timber.d("Msg: user %s scanned, response sent to server: %b", cardResponse, false)
            }
        }
    }

    fun insertInDatabase(cardResponse: Bundle, b: Boolean) {
        val db = AppDatabase.getInstance((ContextProvider.getApplicationContext()))
        val event = Event(
            eventCode = MiroConverter().convertECode(
                cardResponse.get("selection").toString()
            ),
            cardNumber = cardResponse.get("CardCode").toString().toInt(),
            dateTime = cardResponse.get("DateTime").toString(),
            published = b,
            uid = 0 //auto-generate
        )
        db.EventDao().insert(event)
    }


}