package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Event
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.eventRouting() {
    route("/event") {
        get {
            val list = database?.EventDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Event is empty", status = HttpStatusCode.OK)
            }
        }
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val list = database?.EventDao()?.getEventsByCardNumber(id)
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Event is empty", status = HttpStatusCode.OK)
            }
        }
        post {
            val event = call.receive<Event>()
            database?.EventDao()?.insertAll(event)
            call.respondText("Events stored correctly", status = HttpStatusCode.Created)
        }
    }
}