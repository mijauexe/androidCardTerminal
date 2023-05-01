package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.AccessLevel
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import timber.log.Timber

fun Route.accessLevelRouting() {
    route("/accesslevel") {
//        get("/{id?}") {
//            val id = call.parameters["id"]
//            Timber.d("Msg: GET request on /accesslevel/$id")
//            val ac = id?.let { it1 -> database?.AccessLevelDao()?.get(it1.toInt()) }
//            if (ac != null) {
//                call.respond(ac)
//            } else {
//                call.respondText("No access level with id ${id}", status = HttpStatusCode.OK)
//            }
//        }

        post {
            Timber.d("Msg: POST request on /accesslevel")
            val ac = call.receive<AccessLevel>()
            try {
                database?.AccessLevelDao()?.insert(ac)
                call.respondText("Access level stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/list") {
            Timber.d("Msg: POST request on /accesslevel/list")
            val list = call.receive<List<AccessLevel>>()
            try {
                database?.AccessLevelDao()?.insertAll(list)
                call.respondText(
                    "Access level list stored correctly",
                    status = HttpStatusCode.Created
                )
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        delete {
            Timber.d("Msg: DELETE request on /accesslevel")
            val ac = call.receive<AccessLevel>()
            try {
                database?.AccessLevelDao()?.delete(ac)
                call.respondText("Access level deleted correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }
    }
}