package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Event
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import timber.log.Timber

fun Route.eventRouting() {
    route("/event") {
        post {
            Timber.d("Msg: POST request on /event")
            val event = call.receive<Event>()
            try {
                database?.EventDao()?.insert(event)
                call.respondText("Event stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                print(e.printStackTrace())
                call.respondText(
                    e.printStackTrace().toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/addList") {
            Timber.d("Msg: POST request on /event/addList")
            val eventList = call.receive<List<Event>>()
            try {
                database?.EventDao()?.insertAll(eventList)
                call.respondText("Event list stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                print(e.printStackTrace())
                call.respondText(
                    e.printStackTrace().toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        get {
            Timber.d("Msg: GET request on /event")
            val list = database?.EventDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Event db is empty", status = HttpStatusCode.OK)
            }
        }

        get("{id?}") {
            val id = call.parameters["id"]
            Timber.d("Msg: GET request on /event/$id")
            val event = id?.let { it1 -> database?.EventDao()?.get(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with id ${id}", status = HttpStatusCode.OK)
            }
        }

        get("/card_number/{id?}") {
            val id = call.parameters["id"]
            Timber.d("Msg: GET request on /event/card_number/$id")
            val event = id?.let { it1 -> database?.EventDao()?.getEventsByCardNumber(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with card number ${id}", status = HttpStatusCode.OK)
            }
        }

        get("/event_code/{id?}") {
            val id = call.parameters["id"]
            Timber.d("Msg: GET request on /event/event_code/$id")
            val event = id?.let { it1 -> database?.EventDao()?.getEventsByEventCode(it1.toInt()) }
            if (event != null) {
                call.respond(event)
            } else {
                call.respondText("No event with event code ${id}", status = HttpStatusCode.OK)
            }
        }
    }
}