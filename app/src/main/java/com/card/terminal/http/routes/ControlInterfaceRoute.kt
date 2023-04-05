package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Card
import com.card.terminal.utils.MiroConverter
import com.google.gson.Gson
import com.google.gson.JsonParser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import timber.log.Timber

fun Route.controlInterfaceRouting() {
    route("/interface") {
        post {
            try {
                MiroConverter().convertFromAddAll(call.receive())
                call.respondText("Things stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.ServiceUnavailable
                )
            }
        }
    }
}