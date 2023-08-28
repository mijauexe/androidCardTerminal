package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Calendar
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import timber.log.Timber

fun Route.calendarRouting() {
    route("/calendar") {
        get {
            Timber.d("Msg: GET request on /calendar")
            val list = database?.CalendarDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Calendar db is empty", status = HttpStatusCode.OK)
            }
        }
        get("/{id?}") {
            val id = call.parameters["id"]
            Timber.d("Msg: GET request on /calendar/$id")
            val calendar = id?.let { it1 -> database?.CalendarDao()?.get(it1.toInt()) }
            if (calendar != null) {
                call.respond(calendar)
            } else {
                call.respondText("No calendar with id ${id}", status = HttpStatusCode.OK)
            }
        }

        post {
            Timber.d("Msg: POST request on /calendar")
            val calendar = call.receive<Calendar>()
            try {
                database?.CalendarDao()?.insert(calendar)
                call.respondText("Calendar stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/list") {
            Timber.d("Msg: POST request on /calendar/list")
            val list = call.receive<List<Calendar>>()
            try {
                database?.CalendarDao()?.insertAll(list)
                call.respondText("Calendar list stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        delete {
            Timber.d("Msg: DELETE request on /calendar")
            val calendar = call.receive<Calendar>()
            try {
                database?.CalendarDao()?.delete(calendar)
                call.respondText("Deleted correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }
    }
}