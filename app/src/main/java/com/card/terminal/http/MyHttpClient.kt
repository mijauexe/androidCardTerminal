package com.card.terminal.http

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.plugins.configureRouting
import com.card.terminal.http.plugins.configureSerialization
import com.card.terminal.ipOfPCB
import com.card.terminal.main
import com.card.terminal.portOfPCB
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
import java.util.*


object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null
    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope
    private var timer : Timer?= null
    private var delay : Long = 0L
    private var period : Long= 0L
    private var task : TimerTask?= null

    class MyTask : TimerTask() {
        override fun run() {
            readLatestEvent()
        }
    }

    fun bindHttpClient(code: MutableLiveData<Map<String, String>>, appDatabase: AppDatabase) {
        timer = Timer()
        delay = 0L // Initial delay before the task starts
        period = 2000L // Interval between each task in milliseconds

        task = MyTask()

        mutableCode = code
        database = appDatabase
        client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
        }
        execute()
        startLarusTask()
    }

    fun startLarusTask() {
        timer?.scheduleAtFixedRate(task, delay, period)
    }

    fun stopLarusTask() {
        timer?.cancel()
    }

    fun openDoor(doorNum: Int) {
        runBlocking {
            launch(Dispatchers.IO) {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)

                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Opendoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse = getSocketResponse(sendChannel, receiveChannel, buffer)
                println(doorOpenResponse)
                socket.close()
                selectorManager.close()
            }

        }
    }

    fun reset() {
        runBlocking {
            launch(Dispatchers.IO) {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)

                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Reset>".toByteArray()

                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse = getSocketResponse(sendChannel, receiveChannel, buffer)
                println(doorOpenResponse)
                socket.close()
                selectorManager.close()
            }
        }
    }

    fun setDoorTime(
        openDoorTime1: Int,
        openDoorTime2: Int,
        closeDoorTime1: Int,
        closeDoorTime2: Int
    ) {
        runBlocking {
            launch(Dispatchers.IO) {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)

                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)

                var byteArray = "SVA<Doors>".toByteArray()

                val buffer = ByteBuffer.allocate(byteArray.size + 2 + 2 + 1 + 2 + 2 + 1)
                buffer.put(byteArray)

                byteArray = byteArrayOf(
                    (openDoorTime1 shr 0).toByte(),
                    (openDoorTime1 shr 8).toByte(),
                    (closeDoorTime1 shr 0).toByte(),
                    (closeDoorTime1 shr 8).toByte(),
                    0,
                    (openDoorTime2 shr 0).toByte(),
                    (openDoorTime2 shr 8).toByte(),
                    (closeDoorTime2 shr 0).toByte(),
                    (closeDoorTime2 shr 8).toByte(),
                    0
                )

                buffer.put(byteArray)

                buffer.position(0)

//                val doorOpenResponse = getSocketResponse(sendChannel, receiveChannel, buffer)
                sendChannel.writeAvailable(buffer)
//                val oupen = (doorOpenResponse[0].toUByte() + doorOpenResponse[1].toUByte()).toInt()

//                println(doorOpenResponse)
                socket.close()
                selectorManager.close()
            }

        }
    }

    fun readLatestEvent() {
        runBlocking {
            launch(Dispatchers.IO) {
                var selectorManager = SelectorManager(Dispatchers.IO)
                var socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)

                var receiveChannel = socket.openReadChannel()
                var sendChannel = socket.openWriteChannel(autoFlush = true)

                var byteArray = "GVA<Datainfo>".toByteArray()
                var buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val dataInfo = getSocketResponse(sendChannel, receiveChannel, buffer)
                println(dataInfo)

                var lastRead =
                    byteArrayToInt(byteArrayOf(dataInfo[0], dataInfo[1], dataInfo[2], dataInfo[3]))

                val lastSave =
                    byteArrayToInt(byteArrayOf(dataInfo[4], dataInfo[5], dataInfo[6], dataInfo[7]))

                val full = dataInfo[12].toInt()

                socket.close()
                selectorManager.close()


                if (lastRead < lastSave || full == 1) {
                    lastRead += 12
                    selectorManager = SelectorManager(Dispatchers.IO)
                    socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)

                    receiveChannel = socket.openReadChannel()
                    sendChannel = socket.openWriteChannel(autoFlush = true)

                    byteArray = "GVA<Event>".toByteArray()
                    buffer = ByteBuffer.allocate(byteArray.size)
                    buffer.put(byteArray)
                    buffer.position(0)

                    val event = getSocketResponse(sendChannel, receiveChannel, buffer)

                    val eventType = event[0]
                    val cardCode =
                        byteArrayToInt(byteArrayOf(event[1], event[2], event[3], event[4]))

                    println("cardCode:$cardCode")
                    val hour = event[5]
                    val minute = event[6]
                    val second = event[7]
                    val day = event[8]
                    val month = event[9]
                    val year = "20" + event[10]

                    val door = event[11] //uvijek 1??

                    val dateTimeString = "$year-$month-$day" + "T" + "$hour:$minute:$second"

                    println(dateTimeString)
//
                    socket.close()
                    selectorManager.close()

                    selectorManager = SelectorManager(Dispatchers.IO)
                    socket = aSocket(selectorManager).tcp().connect(ipOfPCB, portOfPCB)
//
                    receiveChannel = socket.openReadChannel()
                    sendChannel = socket.openWriteChannel(autoFlush = true)

                    byteArray = "GVA<Dataread>".toByteArray()
                    buffer = ByteBuffer.allocate(5 + byteArray.size)
                    buffer.put(byteArray)

                    byteArray = byteArrayOf(
                        (lastRead shr 0).toByte(),
                        (lastRead shr 8).toByte(),
                        (lastRead shr 16).toByte(),
                        (lastRead shr 24).toByte(),
                        0
                    )
                    buffer.put(byteArray)
                    buffer.position(0)

//                    val movePointer = getSocketResponse(sendChannel, receiveChannel, buffer)
                    sendChannel.writeAvailable(buffer)
//                    println("mp: " + movePointer)

                    socket.close()

                    selectorManager.close()
//
                    mutableCode.postValue(mapOf("CardCode" to cardCode.toString(), "DateTime" to dateTimeString))
                }
            }
        }
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