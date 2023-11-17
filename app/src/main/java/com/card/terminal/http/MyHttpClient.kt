package com.card.terminal.http

import android.content.Context
import android.os.Bundle
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.card.terminal.BuildConfig
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Event
import com.card.terminal.http.plugins.configureRouting
import com.card.terminal.http.plugins.configureSerialization
import com.card.terminal.http.tasks.LarusCheckScansTask
import com.card.terminal.http.tasks.PublishEventsTask
import com.card.terminal.main
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.MiroConverter
import com.card.terminal.utils.Utils
import com.card.terminal.utils.adamUtils.Adam6050D
import com.card.terminal.utils.adamUtils.DigitalOutput
import com.card.terminal.utils.larusUtils.LarusFunctions
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.util.TimerTask
import kotlin.math.pow
import io.ktor.client.engine.okhttp.OkHttpConfig
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

object MyHttpClient {
    private var client: HttpClient? = null
    private var mutableCode = MutableLiveData<Map<String, String>>()
    private var database: AppDatabase? = null
    private lateinit var scope: CoroutineScope
    private var larusCheckScansTask: TimerTask? = null
    private var publishEventsTask: TimerTask? = null
    private var larusFunctions: LarusFunctions? = null
    lateinit var server: NettyApplicationEngine

    private var adamDelayHandler: Handler? = null
    private var adamDelay = 10000L

    internal class AllCertsTrustManager : X509TrustManager {
        @Suppress("TrustAllX509TrustManager")
        override fun checkServerTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
            // no-op
        }

        @Suppress("TrustAllX509TrustManager")
        override fun checkClientTrusted(
            chain: Array<X509Certificate>,
            authType: String
        ) {
            // no-op
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun bindHttpClient(code: MutableLiveData<Map<String, String>>) {
            client = HttpClient {
                install(ContentNegotiation) {
                    json()
                }
                install(HttpRequestRetry) {
                    retryOnServerErrors(maxRetries = 3)
                    exponentialDelay()
                    retryIf { _, response ->
                        !response.status.isSuccess()
                    }
                    retryOnExceptionIf { _, cause ->
                        cause is Exception
                    }
                    delayMillis { retry ->
                        retry * 3000L
                    }
                }
                if(BuildConfig.https) {
                    engine {
                        this as OkHttpConfig
                        config {
                            val trustAllCert = AllCertsTrustManager()
                            val sslContext = SSLContext.getInstance("SSL")
                            sslContext.init(null, arrayOf(trustAllCert), SecureRandom())
                            sslSocketFactory(sslContext.socketFactory, trustAllCert)
                            hostnameVerifier { _, _ -> true }
                        }
                    }
                }
            }
        mutableCode = code

        publishEventsTask = PublishEventsTask()

        database = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
        )

        startNettyServer()

        if (BuildConfig.Larus) {
            larusFunctions = LarusFunctions(client!!, mutableCode)
            larusCheckScansTask = LarusCheckScansTask(larusFunctions!!)
            (larusCheckScansTask as LarusCheckScansTask).startTask()
        }

        (publishEventsTask as PublishEventsTask).startTask()
    }

    fun stopLarusSocket() {
        if (larusCheckScansTask != null) {
            (larusCheckScansTask as LarusCheckScansTask).stopTask()
        }
    }

    fun startLarusSocket() {
        if (!((larusCheckScansTask as LarusCheckScansTask).started)) {
            larusCheckScansTask = LarusCheckScansTask(larusFunctions!!)
            (larusCheckScansTask as LarusCheckScansTask).startTask()
        }
    }

    fun openDoorLarus(doorNum: Int) {
        larusFunctions?.openDoor(doorNum)
    }

    fun openDoorAdam(doorNum: Int) {
        adamDelayHandler?.removeCallbacksAndMessages(null) // Reset the timer
        adamAction(doorNum, 1)

        adamDelayHandler = Handler()
        adamDelayHandler?.postDelayed({
            adamAction(doorNum, 0)
        }, adamDelay)
    }

    private fun adamAction(doorNum: Int, action: Int) {
        val scope3 = CoroutineScope(Dispatchers.IO)
        scope3.launch {
            val ip = BuildConfig.adamIP
            val username = BuildConfig.adamUsername
            val password = BuildConfig.adamPassword

            val adam = Adam6050D(ip, username, password)
            val doOutput = DigitalOutput()

            try {
                doOutput[doorNum] = action
                adam.output(doOutput)
            } catch (e: Exception) {
                Timber.d(e)
            }
        }
    }

    fun checkDoor(doorNum: Int) {
        larusFunctions?.stateDoor(doorNum)
    }

    fun reset() {
        larusFunctions?.reset()
    }

    fun relayMode(doorNum: Int, pulseOrHold: Int) {
        //0 - vrata su normalnom modu rada  - prolaz s karticama, 1 - vrata nisu kontrolirana
        larusFunctions?.changeRelayMode(doorNum, pulseOrHold)
    }

    fun byteArrayToInt(byteArray: ByteArray): Int {
        var result = 0
        var ctr = 0
        for (b in byteArray) {
            result += 2.0.pow(ctr.toDouble()).toInt() * b.toUByte().toInt()
            ctr += 8
        }
        return result
    }

    fun startNettyServer() {
        stop()
        scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            database?.let { main(it) }
            server = embeddedServer(Netty, port = 6969) {
                configureSerialization()
                configureRouting()
            }
            Runtime.getRuntime().addShutdownHook(Thread {
                server.stop(1_000, 2_000)
                Timber.d("Msg: Netty server stopped")
            })
            server.start(wait = true)
            Timber.d("Msg: Netty server started")
        }
    }

    fun stop() {
        if (::server.isInitialized) {
            server.stop(1_000, 2_000)
        }
        try {
            if (this::scope.isInitialized && scope.isActive) {
                scope.cancel()
                Timber.d("Msg: MyHttpClient stopped")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        stopLarusSocket()
    }

    fun isClientReady(): Boolean {
        return client != null
    }

    fun pingEndpoint() {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
//            val larusEndpoint = getPortAndIP()
            val response: HttpResponse =
                client!!.request("192.168.88.219/b0pass/b0pass_iftp2.php") {
                    method = HttpMethod.Get
                }
            println(response)
//            mutableCode.postValue(mapOf("pingResponse" to response.toString()))
        }
    }

    fun pushRequest(type: String) {
        val scope1 = CoroutineScope(Dispatchers.IO)
        scope1.launch {
            val mySharedPreferences = ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)

            var ip = ""
            if(BuildConfig.https) {
                ip = mySharedPreferences.getString("serverIP_s", "?").toString()
            } else {
                ip = mySharedPreferences.getString("serverIP", "?").toString()
            }

            try {
                val response = ip.let {
                    client?.post(it) {
                        contentType(ContentType.Application.Json)
                        if (type.contains("PHOTOS")) {
                            setBody(
                                "{ \n" + "    \"ACT\": \"${type}\", \n" + "    \"IFTTERM2_B0_ID\": \"${
                                    mySharedPreferences.getInt(
                                        "IFTTERM2_B0_ID", 4
                                    )
                                }\",\n" + "    \"BASE64\":\"OFF\"\n" + "}"
                            )
                        } else {
                            setBody(
                                "{\"ACT\": \"${type}\",\"IFTTERM2_B0_ID\": \"${
                                    mySharedPreferences.getInt(
                                        "IFTTERM2_B0_ID", 4
                                    )
                                }\"}"
                            )
                        }
                    }
                }
                if (response != null) {
                    Timber.d("Response received: ${response.bodyAsText()}")
                    Timber.d("Msg: Requested ${type}, got ${response.bodyAsText(Charsets.UTF_8)}")
                    MiroConverter().processRequest(response.bodyAsText())
                }
            } catch (e: NoRouteToHostException) {
                Timber.d(
                    "Msg: No route to host: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            } catch (ce: ConnectException) {
                Timber.d(
                    "Msg: Destination cannot be reached: %s | %s | %s",
                    ce.cause,
                    ce.stackTraceToString(),
                    ce.message
                )
            } catch (e: Exception) {
                Timber.d(
                    "Exception: %s | %s | %s", e.cause, e.stackTraceToString(), e.message
                )
            }
        }
    }

    fun publishNewEvent(cardResponse: Bundle) {
        val scope1 = CoroutineScope(Dispatchers.IO)
        scope1.launch {
            if (cardResponse["noButtonClickNeededRegime"] == true) { //this is needed to avoid null pointer when the flow is fast (no button press needed on the first fragment)
                delay(10000)
            }
            if (cardResponse.containsKey("imageUUID")) {
                try {
                    cardResponse.putString("EventImage",
                        cardResponse.getString("imageUUID")?.let { Utils.findImage(it) })
                } catch (e: Exception) {
                    Timber.d(e.stackTraceToString())
                }
            }
            eventToDatabase(cardResponse, false)

            val body = withContext(Dispatchers.Main) {
                MiroConverter().pushEventFormat(cardResponse)
            }
            val mySharedPreferences = ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            try {
//                delay(2000) FOR TESTING ONLY
//                eventToDatabase(cardResponse, true)

                var ip = ""
                if(BuildConfig.https) {
                    ip = mySharedPreferences.getString("serverIP_s", "?").toString()
                } else {
                    ip = mySharedPreferences.getString("serverIP", "?").toString()
                }

                val response = ip.let {
                    client?.post(it) {
                        contentType(ContentType.Application.Json)
                        setBody(body)
                    }
                }
                if (response != null) {
//                    Timber.d("Sent: ${body}")
//                    Timber.d("Response received: ${response.bodyAsText()}")
//                    Timber.d("Response received status: ${response.status}")
//                    Timber.d("Response received all: ${response}")
                    if (response.bodyAsText().contains("\"CODE\":\"0\"")) {
                        eventToDatabase(cardResponse, true)
                        Timber.d(
                            "Msg: user %s scanned, response sent to server: %b",
                            cardResponse.getString("CardCode"),
                            true
                        )
                    } else {
//                        eventToDatabase(cardResponse, false)
                        Timber.d(
                            "Msg: user %s scanned, response sent to server: %b",
                            cardResponse.getString("CardCode"),
                            false
                        )
                    }
                }
            } catch (ce: ConnectException) {
                Timber.d(
                    "Msg: user %s scanned, response sent to server: %b",
                    cardResponse.getString("CardCode"),
                    false
                )
//                eventToDatabase(cardResponse, false)
            } catch (e: Exception) {
                Timber.d(
                    "Exception while publishing event(s) to server: %s | %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message,
                    body
                )
//                eventToDatabase(cardResponse, false)
                Timber.d(
                    "Msg: user %s scanned, response sent to server: %b",
                    cardResponse.getString("CardCode"),
                    false
                )
            }
        }
    }

    fun publishUnpublishedEvents() {
        val scopeSven = CoroutineScope(Dispatchers.IO)
        scopeSven.launch {
            val mySharedPreferences = withContext(Dispatchers.Main) {
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            }

            val esp = MiroConverter().getFormattedUnpublishedEvents(
                mySharedPreferences.getInt(
                    "IFTTERM2_B0_ID", 0
                )
            )

            if (esp.eventList.isNotEmpty()) {
                try {
                    var ip = ""
                    if(BuildConfig.https) {
                        ip = mySharedPreferences.getString("serverIP_s", "?").toString()
                    } else {
                        ip = mySharedPreferences.getString("serverIP", "?").toString()
                    }

                    val response = ip.let {
                        client?.post(it) {
                            contentType(ContentType.Application.Json)
                            setBody(esp.eventString)
                        }
                    }

                    if (response != null) {
//                        Timber.d("Sent: ${esp.eventString}")
//                        Timber.d("Unpublished events sent to " +  mySharedPreferences.getString(
//                            "serverIP", ""))
//                        Timber.d("Unpublished events response: ${response.bodyAsText()}")
//                        Timber.d("Unpublished events Response received status: ${response.status}")
//                        Timber.d("Unpublished events Response received headers: ${response.headers}")
                        if (response.bodyAsText().contains("\"CODE\":\"0\"")) {
                            updateEvents(esp.eventList)
                            Timber.d(
                                "Msg: Event list updated and published: %s", esp.eventString.length
                            )
                        }
                    }
                } catch (ce: ConnectException) {
                    Timber.d("Msg: Event list not updated or published: %s", esp.eventString.length)
                } catch (e: Exception) {
                    Timber.d(
                        "Exception while publishing unpublished event(s) to server: %s | %s | %s | %s",
                        e.cause,
                        e.stackTraceToString(),
                        e.message,
                        esp.eventString
                    )
                }
            }
        }
    }

    fun eventToDatabase(cardResponse: Bundle, published: Boolean) {
        if (cardResponse["CardCode"] != null) {
            val scope1 = CoroutineScope(Dispatchers.IO)
            scope1.launch {

                val mySharedPreferences = withContext(Dispatchers.Main) {
                    ContextProvider.getApplicationContext()
                        .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                }

                val db = AppDatabase.getInstance(
                    ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
                )
                if (published) {
                    val e = db.EventDao()
                        .getLastScanEventWithCardNumber(Integer.valueOf(cardResponse.get("CardCode") as String))
                    val newE = e?.let {
                        Event(
                            uid = it.uid,
                            eventCode = it.eventCode,
                            eventCode2 = it.eventCode2,
                            cardNumber = it.cardNumber,
                            dateTime = it.dateTime,
                            published = true,
                            deviceId = it.deviceId,
                            image = cardResponse.getString("imageUUID", "")
                        )
                    }
                    if (newE != null) {
                        db.EventDao().update(newE)
                    }
                } else {
                    val event = Event(
                        eventCode = cardResponse.getInt("eCode"), //TODO
                        eventCode2 = cardResponse.getInt("eCode2", 0),
                        cardNumber = cardResponse.get("CardCode").toString().toInt(),
                        dateTime = cardResponse.get("DateTime").toString(),
                        published = false,
                        uid = 0, //auto-generate
                        deviceId = mySharedPreferences.getInt("IFTTERM2_B0_ID", 0),//TODO
                        image = cardResponse.getString("EventImage", "")
                    )
                    db.EventDao().insert(event)
                }
            }
        }
    }

    private fun updateEvents(list: List<Event>) {
        val newEvents = mutableListOf<Event>()
        for (e in list) {
            newEvents.add(
                Event(
                    uid = e.uid,
                    eventCode = e.eventCode,
                    eventCode2 = e.eventCode2,
                    cardNumber = e.cardNumber,
                    dateTime = e.dateTime,
                    published = true,
                    deviceId = 0, //TODO
                    image = e.image
                )
            )
        }
        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
        )
        db.EventDao().updateEvents(newEvents)
    }
}