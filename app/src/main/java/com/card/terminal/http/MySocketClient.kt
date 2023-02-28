package com.card.terminal.http

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class MySocketClient(private val ipAddress: String, private val port: Int) {
    private lateinit var socketChannel: SocketChannel
    private var clientJob: Job? = null

    fun startClient() {
//        clientJob = GlobalScope.launch(Dispatchers.IO) {
        socketChannel = SocketChannel.open()
        socketChannel.connect(InetSocketAddress(ipAddress, port))

        // Coroutine to read data from the server
//            launch { readData() }
//        }
    }

    suspend fun sendData(data: String) {
        val buffer = ByteBuffer.wrap(data.toByteArray())
        withContext(Dispatchers.IO) {
            socketChannel.write(buffer)
        }
    }

    suspend fun readData() {
        while (true) {
            val buffer = ByteBuffer.allocate(1024)
            withContext(Dispatchers.IO) {
                val bytesRead = socketChannel.read(buffer)
                if (bytesRead == -1) {
                    // End of stream, break the loop
                    return@withContext
                }
                val data = buffer.array().copyOfRange(0, bytesRead)
                processData(data)
            }
        }
    }

    fun processData(data: ByteArray) {
        // Process the received data here
        println("Received data: ${String(data)}")
    }

    fun stopClient() {
        runBlocking {
            clientJob?.cancelAndJoin()
            socketChannel.close()
        }
    }
}
