package com.card.terminal.http

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.main
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object MyHttpClient {
    private var client: HttpClient? = null

    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var mContext: Application? = null
    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope

    fun bindHttpClient(code: MutableLiveData<Map<String, String>>, appDatabase: AppDatabase ) {
        mutableCode = code
        database = appDatabase
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
            database?.let { main(it) }
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