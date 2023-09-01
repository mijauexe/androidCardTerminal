package com.card.terminal.utils.adamUtils

import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.util.Base64

class Requestor(ip: String, username: String, password: String) {

    private val headers: Map<String, String>
    private val baseUrl: String

    init {
        val authStr = "$username:$password"
        val encodedAuthStr =
            Base64.getEncoder().encodeToString(authStr.toByteArray(Charsets.US_ASCII))
        headers = mapOf(
            "Content-Type" to "application/x-www-form-urlencoded",
            "Authorization" to "Basic $encodedAuthStr"
        )
        baseUrl = "http://$ip"
    }

    fun input(inputChannelId: Int? = null): String {
        println("inputChannelId " + inputChannelId)
        val url = if (inputChannelId != null) {
            "$baseUrl${URI.DIGITAL_INPUT}/$inputChannelId${URI.VALUE}"
        } else {
            "$baseUrl${URI.DIGITAL_INPUT}${URI.ALL}${URI.VALUE}"
        }
        println("url je " + url)

        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        connection.setRequestProperty("Authorization", headers["Authorization"])
        return connection.inputStream.bufferedReader().use { it.readText() }
    }

    fun output(data: Map<String, Int>? = null): String {
        val url = "$baseUrl${URI.DIGITAL_OUTPUT}${URI.ALL}${URI.VALUE}"

        var params = ""

        val request = if (data != null) {
            params = data.map { "${it.key}=${it.value}" }.joinToString("&")

            Request.Builder().url(url)
                .post(params.toRequestBody("application/x-www-form-urlencoded".toMediaType()))
                .headers(headers.toHeaders()).build()
        } else {
            Request.Builder().url(url).get().headers(headers.toHeaders()).build()
        }

        return httpRequest(url, params)

//        val response = OkHttpClient().newCall(request).execute()
//
//        return response.body?.string() ?: ""
//        return try {
//            val response = OkHttpClient().newCall(request).execute()
//            response.body?.string() ?: ""
//        } catch (e : SocketException) {
//            println(e)
//            ""
//        }
    }

    fun httpRequest(host: String, data: String): String {
        try {
            val connection = URL(host).openConnection() as HttpURLConnection

            // Set headers if needed
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            connection.setRequestProperty("Authorization", "Basic cm9vdDowMDAwMDAwMA==")

            if (data != "") {
                // Set the request method to POST
                connection.requestMethod = "POST"
                connection.doOutput = true
                val os: OutputStream = connection.outputStream
                os.write(data.toByteArray(Charsets.UTF_8))
                os.close()
            } else {
                connection.requestMethod = "GET"
            }

            val responseCode = connection.responseCode

            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    response.append(line).append('\n')
                }

                reader.close()
                connection.disconnect()

                return response.toString()
            }
        } catch (e: NoRouteToHostException) {
            println(e) //TODO HANDLE NO CONNECTION PROPERLY
        } catch (e: Exception) {
            println(e)
        }

        return ""
    }
}

object URI {
    const val DIGITAL_INPUT = "/digitalinput"
    const val DIGITAL_OUTPUT = "/digitaloutput"
    const val ALL = "/all"
    const val VALUE = "/value"
}