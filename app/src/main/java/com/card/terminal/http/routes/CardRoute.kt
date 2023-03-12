package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Card
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.cardRouting() {
    route("/card") {
        get {
            val list = database?.CardDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Card is empty", status = HttpStatusCode.OK)
            }
        }
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val list = database?.CardDao()?.getCardsByPersonId(id.toInt())
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Card is empty", status = HttpStatusCode.OK)
            }
        }
        post {
            val card = call.receive<Card>()
            database?.CardDao()?.insertAll(card)
            call.respondText("Cards stored correctly", status = HttpStatusCode.Created)
        }
//        delete("{id?}") {
//            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
//            if (readInfoStorage.removeIf { it.uid == Integer.parseInt(id) }) {
//                call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
//            } else {
//                call.respondText("Not Found", status = HttpStatusCode.NotFound)
//            }
//        }
    }
}