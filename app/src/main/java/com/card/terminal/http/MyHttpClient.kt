package com.card.terminal.http

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.card.terminal.db.AppDatabase
import com.card.terminal.main
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
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

    fun execute() {
        scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            database?.let { main(it) }
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