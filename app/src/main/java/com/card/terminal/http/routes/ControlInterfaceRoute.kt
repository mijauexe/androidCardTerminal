package com.card.terminal.http.routes

import com.card.terminal.utils.MiroConverter
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.controlInterfaceRouting() {
    route("/interface") {
        post {
            try {
                val response = MiroConverter().processRequest(call.receive())
                call.respondText(response, status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.ServiceUnavailable
                )
            }
        }


    }


}