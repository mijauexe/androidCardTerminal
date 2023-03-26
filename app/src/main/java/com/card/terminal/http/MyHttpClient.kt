package com.card.terminal.http

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.plugins.configureRouting
import com.card.terminal.http.plugins.configureSerialization
import com.card.terminal.http.tasks.LarusCheckScansTask
import com.card.terminal.main
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.larusUtils.LarusFunctions
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
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
        }

        mutableCode = code

        larusFunctions = LarusFunctions(client!!, mutableCode)
        larusCheckScansTask = LarusCheckScansTask(larusFunctions!!)

        database = AppDatabase.getInstance((ContextProvider.getApplicationContext()))

        startNettyServer()

        (larusCheckScansTask as LarusCheckScansTask).startTask()
    }

    fun pingy(cardNumber : String) {
        larusFunctions?.openDoor(1)
        //postData(mapOf("test" to "sven", "test1" to "miro"))
        //postData(mapOf("ACT" to "NEW_EVENTS", mapOf<String, Map>("CREAD" to )))
        postString(cardNumber)
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
                server.stop(1000, 5000)
            })
            server.start(wait = true)
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

    fun isClientReady(): Boolean {
        return client != null
    }

    fun postData(cardResponseMap: Map<String, String>) {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val response = client?.post("http://sucic.info/b0pass/b0pass_iftp2.php?act=IFTTERM2_REQUEST") {
                contentType(ContentType.Application.Json)
                setBody(cardResponseMap)
            }
            if (response != null) {
                println(response)
                println(response.bodyAsText())
            }
        }
    }

    fun postString(cardResponse: String) {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val response = client?.post("http://sucic.info/b0pass/b0pass_iftp2.php") {
                contentType(ContentType.Application.Json)
                val str = "{\"ACT\": \"NEW_EVENTS\",\n" +
                        "\"CREAD\":[{\"CN\":\"${cardResponse}\", \"GENT\":\"2023-03-23T15:00:05\", \"ECODE\":\"1\"}]" +
                        "}"
                setBody(str)
            }
            if (response != null) {
                println(response)
                println(response.bodyAsText())
            }
        }
    }
}