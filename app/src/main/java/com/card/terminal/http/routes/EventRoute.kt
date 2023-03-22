package com.card.terminal.http.routes

import com.card.terminal.database
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.eventRouting() {
    route("/event") {
        get {
            val list = database?.EventDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Event db is empty", status = HttpStatusCode.OK)
            }
        }

        get("{id?}") {
            val id = call.parameters["id"]
            val event = id?.let { it1 -> database?.EventDao()?.get(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with id ${id}", status = HttpStatusCode.OK)
            }
        }

        get("/card_number/{id?}") {
            val id = call.parameters["id"]
            val event = id?.let { it1 -> database?.EventDao()?.getEventsByCardNumber(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with card number ${id}", status = HttpStatusCode.OK)
            }
        }

        get("/event_code/{id?}") {
            val id = call.parameters["id"]
            val event = id?.let { it1 -> database?.EventDao()?.getEventsByEventCode(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with event code ${id}", status = HttpStatusCode.OK)
            }
        }
    }
}