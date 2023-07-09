package com.card.terminal.utils.larusUtils

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.MutableLiveData
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import io.ktor.client.HttpClient
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class LarusFunctions(
    val client: HttpClient,
    val mutableCode: MutableLiveData<Map<String, String>>
) {


    data class LarusEndpoint(var ip: String, var port: Int)

    fun getPortAndIP(prefs: SharedPreferences): LarusEndpoint {
//        val sharedPreferences = ContextProvider.getApplicationContext()
//            .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
        val ip = prefs.getString("larusIP", "0")
        val port = prefs.getInt("larusPort", 8005)
        return LarusEndpoint(ip!!, port)
    }

    fun readLatestEvent() {
        var lastRead: Int
        var lastSave: Int
        var full: Int
        val scope = CoroutineScope(Dispatchers.IO)

        scope.launch {
            val sharedPreferences = ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            var socket1: Socket? = null
            val selectorManager = SelectorManager(Dispatchers.IO)
            try {
                val larusEndpoint = getPortAndIP(sharedPreferences)

                val socket = aSocket(selectorManager).tcp()

                withTimeout(2000) {
                    socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                }

                var receiveChannel = socket1!!.openReadChannel()
                var sendChannel = socket1!!.openWriteChannel(autoFlush = true)

                var byteArray = "GVA<Datainfo>".toByteArray()
                var buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val dataInfo =
                    getSocketResponse("readLatestEvent", sendChannel, receiveChannel, buffer)

                withContext(Dispatchers.IO) {
                    socket1?.close()
                }

                if (dataInfo.size == 13) {
//                    println(LocalTime.now())
//                    for (i in dataInfo) {
//                        print("$i ")
//                    }
//                    println()

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
                        socket1?.close()
                    }

                    if (sharedPreferences.getBoolean(
                            "Connection",
                            false
                        ) == false && lastSave != 0
                    ) {

                        //TODO OVO TREBA POPRAVIT
                        //ako je veza bila pukuta (false), a sad je socket uspio proc ->
                        //treba stavit lastRead na lastSave i promijeniti connection u true
                        withTimeout(2000) {
                            socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                        }
                        sendChannel = socket1!!.openWriteChannel(autoFlush = true)

                        byteArray = "GVA<Dataread>".toByteArray()
                        buffer = ByteBuffer.allocate(5 + byteArray.size)
                        buffer.put(byteArray)

                        byteArray = byteArrayOf(
                            (lastSave shr 0).toByte(),
                            (lastSave shr 8).toByte(),
                            (lastSave shr 16).toByte(),
                            (lastSave shr 24).toByte(),
                            0
                        )

                        buffer.put(byteArray)
                        buffer.position(0)

                        sendChannel.writeAvailable(buffer)

                        withContext(Dispatchers.IO) {
                            socket1?.close()
                        }

                        val editor = sharedPreferences.edit()
                        editor.putBoolean("Connection", true)
                        editor.apply()
                        mutableCode.postValue(
                            mapOf(
                                "CardCode" to "CONNECTION_RESTORED"
                            )
                        )
                    } else if (lastRead < lastSave || full == 1) {
                        lastRead += 12

                        withTimeout(2000) {
                            socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                        }

                        receiveChannel = socket1!!.openReadChannel()
                        sendChannel = socket1!!.openWriteChannel(autoFlush = true)

                        byteArray = "GVA<Event>".toByteArray()
                        buffer = ByteBuffer.allocate(byteArray.size)
                        buffer.put(byteArray)
                        buffer.position(0)
                        val event =
                            getSocketResponse(
                                "readLatestEvent",
                                sendChannel,
                                receiveChannel,
                                buffer
                            )

                        if (event.size >= 4) {
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

                            val dateTimeString = LocalDateTime.now()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                                .toString()

                            println(dateTimeString)

                            withContext(Dispatchers.IO) {
                                socket1?.close()
                            }
                            withTimeout(2000) {
                                socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                            }
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
                                socket1?.close()
                            }

                            if (cardCode.toString()
                                    .equals("1094999887") || cardCode.toString().length > 7
                            ) {
                                Timber.d("Memorija: ")
                                for (i in dataInfo) {
                                    Timber.d("$i ")
                                }
                                Timber.d("")
                            }

                            if(cardCode.toString() != "1094999887") {
                                mutableCode.postValue(
                                    mapOf(
                                        "CardCode" to cardCode.toString(),
                                        "DateTime" to dateTimeString
                                    )
                                )
                            }
                        }
                    }
                }
                withContext(Dispatchers.IO) {
                    socket1?.close()
                }
            } catch (e: IOException) {
                if (sharedPreferences.getBoolean("Connection", false)) {
                    Timber.d(
                        "Msg: IOException %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                }
            } catch (e: NoRouteToHostException) {
                if (sharedPreferences.getBoolean("Connection", false)) {
                    Timber.d(
                        "Msg: NoRouteToHostException %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("Connection", false)
                    editor.apply()
                    mutableCode.postValue(
                        mapOf(
                            "CardCode" to "CONNECTION_LOST"
                        )
                    )
                }
            } catch (e: ConnectException) {
                if (sharedPreferences.getBoolean("Connection", false)) {
                    Timber.d(
                        "Msg: ConnectException %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message
                    )
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("Connection", false)
                    editor.apply()
                    mutableCode.postValue(
                        mapOf(
                            "CardCode" to "CONNECTION_LOST"
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {

            } catch (e: Exception) {
                Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            } finally {
                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
            }
        }
    }

    suspend fun getSocketResponse(
        fn: String,
        sendChannel: ByteWriteChannel,
        receiveChannel: ByteReadChannel,
        buffer: ByteBuffer
    ): ByteArray {
        sendChannel.writeAvailable(buffer)

        var byteResponseArray = byteArrayOf()

        while (true) {
            try {
                val response = receiveChannel.readByte()
                byteResponseArray += response
            } catch (e: Exception) {
                if (fn.equals("openDoor")) {
                    Timber.d("${fn}, | ${e}")
                }
                break
            }
        }
        return byteResponseArray
    }

    fun openDoor(doorNum: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            var socket1: Socket? = null
            val selectorManager = SelectorManager(Dispatchers.IO)
            try {
                Timber.d("Pokusavam Door ${doorNum} opened.")
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                val socket1: Socket?

                withTimeout(2000) {
                    socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                }
                Timber.d("Pokusavam1 Door ${doorNum} opened.")

                val receiveChannel = socket1?.openReadChannel()
                val sendChannel = socket1?.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Opendoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)
                Timber.d("Pokusavam2 Door ${doorNum} opened.")

                val doorOpenResponse =
                    getSocketResponse("openDoor", sendChannel!!, receiveChannel!!, buffer)
                Timber.d("Pokusavam3 Door ${doorNum} opened.")
                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
                Timber.d("Door ${doorNum} opened.")
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
                Timber.d("Msg: TimeoutCancellationException to larus board")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
                Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            } finally {
                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
            }
        }
    }

    fun reset() {
        runBlocking {
            launch(Dispatchers.IO) {
                val selectorManager = SelectorManager(Dispatchers.IO)
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)
                val socket =
                    aSocket(selectorManager).tcp().connect(larusEndpoint.ip, larusEndpoint.port)

                val receiveChannel = socket.openReadChannel()
                val sendChannel = socket.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Reset>".toByteArray()

                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse =
                    getSocketResponse("reset", sendChannel, receiveChannel, buffer)
                println(doorOpenResponse)
                socket.close()
                selectorManager.close()
                Timber.d("Msg: Reset command")
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
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)

                val socket =
                    aSocket(selectorManager).tcp().connect(larusEndpoint.ip, larusEndpoint.port)

//                val receiveChannel = socket.openReadChannel()
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
                Timber.d(
                    "Msg: Set door time to %d %d %d %d",
                    openDoorTime1,
                    openDoorTime2,
                    closeDoorTime1,
                    closeDoorTime2
                )
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

    fun GVAHoldDoor(doorNum: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                val socket1: Socket?

                withTimeout(2000) {
                    socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                }

                val receiveChannel = socket1?.openReadChannel()
                val sendChannel = socket1?.openWriteChannel(autoFlush = true)

                val byteArray = "GVA<Holddoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse =
                    getSocketResponse("GVAHoldDoor", sendChannel!!, receiveChannel!!, buffer)
                println(doorOpenResponse)
                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
                Timber.d("Msg: TimeoutCancellationException to larus board")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
                Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            }
            Timber.d("Msg: Door $doorNum opened")
        }
    }

    fun stateDoor(doorNum: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                val socket1: Socket?

                withTimeout(2000) {
                    socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                }

                val receiveChannel = socket1?.openReadChannel()
                val sendChannel = socket1?.openWriteChannel(autoFlush = true)

                val byteArray = "GVA<Statedoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size)
                buffer.put(byteArray)
                buffer.position(0)

                val doorOpenResponse =
                    getSocketResponse("stateDoor", sendChannel!!, receiveChannel!!, buffer)
                println(doorOpenResponse)
                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
                Timber.d("Relay ${doorNum} in state: ${doorOpenResponse}")
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
                Timber.d("Msg: TimeoutCancellationException to larus board")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
                Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            }
            Timber.d("Msg: Door $doorNum opened")
        }
    }

    fun changeRelayMode(doorNum: Int, pulseOrHold: Int) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val sharedPreferences = ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                val larusEndpoint = getPortAndIP(sharedPreferences)
                val selectorManager = SelectorManager(Dispatchers.IO)
                val socket = aSocket(selectorManager).tcp()
                val socket1: Socket?

                withTimeout(2000) {
                    socket1 = socket.connect(larusEndpoint.ip, larusEndpoint.port)
                }

                val sendChannel = socket1?.openWriteChannel(autoFlush = true)

                val byteArray = "SVA<Holddoor$doorNum>".toByteArray()
                val buffer = ByteBuffer.allocate(byteArray.size + 1)
                buffer.put(byteArray)
                buffer.put(pulseOrHold.toByte()) //0 - vrata su normalnom modu rada  - prolaz s karticama, 1 - vrata nisu kontrolirana
                buffer.position(0)

                sendChannel!!.writeAvailable(buffer)

                withContext(Dispatchers.IO) {
                    socket1?.close()
                    selectorManager?.close()
                }
                Timber.d("Msg: Relay $doorNum set to ${pulseOrHold}")
            } catch (e: TimeoutCancellationException) {
                println("TimeoutCancellationException: ${e.message}")
                Timber.d("Msg: TimeoutCancellationException to larus board")
            } catch (e: Exception) {
                println("Exception: ${e.message}")
                Timber.d("Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
            }
        }
    }


}


