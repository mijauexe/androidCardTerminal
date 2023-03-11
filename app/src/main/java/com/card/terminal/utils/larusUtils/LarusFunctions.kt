package com.card.terminal.utils.larusUtils

import androidx.lifecycle.MutableLiveData
import com.card.terminal.http.MyHttpClient
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import java.nio.ByteBuffer

class LarusFunctions(
    val client: HttpClient,
    val mutableCode: MutableLiveData<Map<String, String>>
) {

    fun pingEndpoint() {
        runBlocking {
            launch(Dispatchers.IO) {
                val response: HttpResponse = client.request("192.168.0.200") {
                    method = HttpMethod.Get
                }
                mutableCode.postValue(mapOf("response" to response.toString()))
            }
        }
    }

    fun readLatestEvent() {
        var lastRead = 0
        var lastSave = 0
        var full = 0
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {

                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                var socket1: Socket?
                withTimeout(2000) {
                    socket1 = socket.connect("192.168.0.200", 8005)
                }
                var receiveChannel = socket1!!.openReadChannel()
                var sendChannel = socket1!!.openWriteChannel(autoFlush = true)

                var byteArray = "GVA<Datainfo>".toByteArray()
                var buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val dataInfo =
                    MyHttpClient.getSocketResponse(sendChannel, receiveChannel, buffer)
                println(dataInfo)

                lastRead =
                    MyHttpClient.byteArrayToInt(
                        byteArrayOf(
                            dataInfo[0],
                            dataInfo[1],
                            dataInfo[2],
                            dataInfo[3]
                        )
                    )

                lastSave =
                    MyHttpClient.byteArrayToInt(
                        byteArrayOf(
                            dataInfo[4],
                            dataInfo[5],
                            dataInfo[6],
                            dataInfo[7]
                        )
                    )

                full = dataInfo[12].toInt()

                withContext(Dispatchers.IO) {
                    socket1!!.close()
                }

                if (lastRead < lastSave || full == 1) {
                    lastRead += 12

                    withTimeout(2000) {
                        socket1 = socket.connect("192.168.0.200", 8005)
                    }
                    receiveChannel = socket1!!.openReadChannel()
                    sendChannel = socket1!!.openWriteChannel(autoFlush = true)

                    byteArray = "GVA<Event>".toByteArray()
                    buffer = ByteBuffer.allocate(byteArray.size)
                    buffer.put(byteArray)
                    buffer.position(0)

                    val event =
                        MyHttpClient.getSocketResponse(sendChannel, receiveChannel, buffer)

                    val eventType = event[0]
                    val cardCode =
                        MyHttpClient.byteArrayToInt(
                            byteArrayOf(
                                event[1],
                                event[2],
                                event[3],
                                event[4]
                            )
                        )

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

                    withContext(Dispatchers.IO) {
                        socket1!!.close()
                    }

                    withTimeout(2000) {
                        socket1 = socket.connect("192.168.0.200", 8005)
                    }
                    receiveChannel = socket1!!.openReadChannel()
                    sendChannel = socket1!!.openWriteChannel(autoFlush = true)

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

                    sendChannel.writeAvailable(buffer)

                    withContext(Dispatchers.IO) {
                        socket1!!.close()
                    }

                    mutableCode.postValue(
                        mapOf(
                            "CardCode" to cardCode.toString(),
                            "DateTime" to dateTimeString
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
            }
        }
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

    fun openDoor(doorNum: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                val socket1: Socket?
                withTimeout(2000) {
                    socket1 = socket.connect("192.168.0.200", 8005)
                }

                val receiveChannel = socket1?.openReadChannel()
                val sendChannel = socket1?.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Opendoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse = getSocketResponse(sendChannel!!, receiveChannel!!, buffer)
                println(doorOpenResponse)
                withContext(Dispatchers.IO) {
                    socket1.close()
                    selectorManager.close()
                }
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
            }
        }
    }

    fun reset() {
        runBlocking {
            launch(Dispatchers.IO) {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp().connect("192.168.0.200", 8005)


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
                val socket = aSocket(selectorManager).tcp().connect("192.168.0.200", 8005)


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

    fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        var ctr = 0
        for (b in byteArray) {
            result += Math.pow(2.0, ctr.toDouble()).toInt() * b.toUByte().toInt()
            ctr += 8
        }
        return result
    }

}


