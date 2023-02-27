package com.card.terminal.http

import android.app.Activity
import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.plugins.configureRouting
import com.card.terminal.http.plugins.configureSerialization
import com.card.terminal.main
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.nio.ByteBuffer

object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null
    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope
    fun bindHttpClient(code: MutableLiveData<Map<String, String>>, appDatabase: AppDatabase) {
        mutableCode = code
        database = appDatabase
        client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
        }
        execute()
    }

    suspend fun communicateWithTeo(act: Activity) {
        runBlocking {
            val selectorManager = SelectorManager(Dispatchers.IO)
            val socket = aSocket(selectorManager).tcp().connect("192.168.0.200", 8005)

            val receiveChannel = socket.openReadChannel()
            val sendChannel = socket.openWriteChannel(autoFlush = true)

            launch(Dispatchers.IO) {
                //read
                val dataInfo = getSocketResponse(sendChannel, receiveChannel, "GVA<Datainfo>>")
                println(dataInfo)

                val lastRead = dataInfo[0] + dataInfo[1] + dataInfo[2] + dataInfo[3]
                val lastSave = dataInfo[4] + dataInfo[5] + dataInfo[6] + dataInfo[7]
                val numData = dataInfo[8] + dataInfo[9]
                val maxData = dataInfo[10] + dataInfo[11]
                val full = dataInfo[12]

                //pointer
                val pointerC = pack("SVA<Dataread>", "VC", lastRead, full)
                val pointerCResponse =
                    getSocketResponse(sendChannel, receiveChannel, pointerC.toString())
                println(pointerCResponse)

                //write
                val writeC =
                    pack("SVA<Datainfo>", "VVvvC", lastRead, lastSave, numData, maxData, full)
                val writeCResponse =
                    getSocketResponse(sendChannel, receiveChannel, writeC.toString())
                println(writeCResponse)

                //write
                val writeC1 =
                    pack("GVA<Datainfo>", "VVvvC", lastRead, lastSave, numData, maxData, full)
                val writeC1Response =
                    getSocketResponse(sendChannel, receiveChannel, writeC1.toString())
                println(writeC1Response)

                //read event
                val readEvent = getSocketResponse(sendChannel, receiveChannel, "GVA<Event>")
                println(readEvent)

//                Handler(Looper.getMainLooper()).post {
//                    Toast.makeText(act, String(event), Toast.LENGTH_LONG).show()
//                }

                socket.close()
                selectorManager.close()
            }
        }

    }

    fun pack(vararg args: Any): ByteArray {
        val buffer = ByteBuffer.allocate(args.sumOf { sizeOf(it) })
        for (arg in args) {
            when (arg) {
                is Byte -> buffer.put(arg)
                is Short -> buffer.putShort(arg)
                is Int -> buffer.putInt(arg)
                is Long -> buffer.putLong(arg)
                is Float -> buffer.putFloat(arg)
                is Double -> buffer.putDouble(arg)
                is Char -> buffer.putChar(arg)
                is String -> buffer.put(arg.toByteArray())
                is ByteArray -> buffer.put(arg)
                else -> throw IllegalArgumentException("Invalid argument type: ${arg.javaClass}")
            }
        }
        return buffer.array()
    }

    private fun sizeOf(arg: Any): Int {
        return when (arg) {
            is Byte, is Char -> 1
            is Short -> 2
            is Int, is Float -> 4
            is Long, is Double -> 8
            is String -> arg.length
            is ByteArray -> arg.size
            else -> throw IllegalArgumentException("Invalid argument type: ${arg.javaClass}")
        }
    }

    suspend fun getSocketResponse(
        sendChannel: ByteWriteChannel,
        receiveChannel: ByteReadChannel,
        msg: String
    ): ByteArray {
        sendChannel.writeStringUtf8(msg)
        receiveChannel.awaitContent()
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

    fun execute() {
        stop()
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            database?.let { main(it) }
            embeddedServer(Netty, port = 6969) {
                configureSerialization()
                configureRouting()
            }.start(wait = true)
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
            val response = client?.post("http://192.168.0.188:5000") {
                contentType(ContentType.Application.Json)
                setBody(cardResponseMap)
            }
            if (response != null) {
                println(response)
            }
        }
    }
}