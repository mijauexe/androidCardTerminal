package com.card.terminal.http.routes

import com.card.terminal.utils.MiroConverter
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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