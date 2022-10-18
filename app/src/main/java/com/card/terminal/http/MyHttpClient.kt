package com.card.terminal.http

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null

    private lateinit var scope: CoroutineScope

    fun bindHttpClient(code: MutableLiveData<Map<String, String>>) {
        mutableCode = code
        client = HttpClient() {
            install(ContentNegotiation) {
                json()
            }
        }
        execute(mutableCode)
    }

    fun execute(mutableCode: MutableLiveData<Map<String, String>>) {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            //greeting(mutableCode)


            embeddedServer(Netty, port = 5005) {
                routing {
                    get("/") {
                        Handler(Looper.getMainLooper()).post {
                            //Toast.makeText(this.context, "pingao te netko", Toast.LENGTH_LONG).show()
                            mutableCode.postValue(mapOf("MESSAGE" to "netko te pingao"))
                        }
                        delay(3000)
                        Handler(Looper.getMainLooper()).post {
                            //Toast.makeText(this.context, "pingao te netko", Toast.LENGTH_LONG).show()
                            mutableCode.postValue(mapOf("MESSAGE" to ""))
                        }
                    }
                }
            }.start(wait = true)

        }
    }

    suspend fun greeting(cardResponseMap: Map<String, String>): String {
        //val response = client?.get("http://192.168.0.188:5000")
        //val response = client?.get("https://www.google.com")

        val response = client?.post("http://192.168.0.188:5000") {
            contentType(ContentType.Application.Json)
            setBody(cardResponseMap)
        }

        if (response != null) {
            println(response)
            //mutableCode.postValue(Pair(ServerStatus.MESSAGE, response.toString()))
            //mutableCode.postValue(mapOf("MESSAGE" to response.toString()))
            return response.bodyAsText()
        }
        return ""
    }


}