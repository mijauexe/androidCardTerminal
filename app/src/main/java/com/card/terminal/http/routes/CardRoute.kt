package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Card
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

fun Route.cardRouting() {
    route("/card") {
        get {
            Timber.d("Msg: GET request on /card")
            val list = database?.CardDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Card db is empty", status = HttpStatusCode.OK)
            }
        }
//        get("/{id?}") {
//            val id = call.parameters["id"]
//            Timber.d("Msg: GET request on /card/$id")
//            val card = id?.let { it1 -> database?.CardDao()?.get(it1.toInt()) }
//            if (card != null) {
//                call.respond(card)
//            } else {
//                call.respondText("No card with card number ${id}", status = HttpStatusCode.OK)
//            }
//        }

        get("/person/{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            Timber.d("Msg: GET request on /card/person/$id")
            val list = database?.CardDao()?.getCardsByPersonId(id.toInt())
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Person with id ${id} has no cards.", status = HttpStatusCode.OK)
            }
        }
        post {
            Timber.d("Msg: POST request on /card")
            val card = call.receive<Card>()
            try {
                database?.CardDao()?.insert(card)
                call.respondText("Card stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/list") {
            Timber.d("Msg: POST request on /card/list")
            val cards = call.receive<List<Card>>()
            try {
                database?.CardDao()?.insertAll(cards)
                call.respondText("Cards stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        delete("/list/owner") {
            Timber.d("Msg: DELETE request on /card/list/owner")
            val listOfIdsToBeDeleted = call.receive<Map<String, List<Int>>>()
            var deletedRow = 0
            val unsuccessful = mutableListOf<Int>()
            try {
                for (values in listOfIdsToBeDeleted.values) {
                    for (v in values) {
                        try {
                            val del = database?.CardDao()?.deleteByOwnerId(v)
                            if (del != 0) {
                                deletedRow += del!!
                            } else {
                                unsuccessful.add(v)
                            }
                        } catch (e: Exception) {
                            call.respondText(
                                e.message.toString(),
                                status = HttpStatusCode.Created
                            )
                        }
                    }
                }
                if (unsuccessful.size != 0) {
                    call.respondText(
                        "$deletedRow row(s) deleted. Values with ids ${unsuccessful} weren't deleted.",
                        status = HttpStatusCode.Created
                    )
                } else {
                    call.respondText(
                        "$deletedRow row(s) deleted.",
                        status = HttpStatusCode.Created
                    )
                }
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(),
                    status = HttpStatusCode.BadRequest
                )
            }
        }

        delete("/list/card_number") {
            Timber.d("Msg: DELETE request on /card/list/card_number")
            val listOfIdsToBeDeleted = call.receive<Map<String, List<Int>>>()
            var deletedRow = 0
            val unsuccessful = mutableListOf<Int>()
            try {
                for (values in listOfIdsToBeDeleted.values) {
                    for (v in values) {
                        try {
                            val del = database?.CardDao()?.deleteByCardNumber(v)
                            if (del == 1) {
                                deletedRow++
                            } else {
                                unsuccessful.add(v)
                            }
                        } catch (e: Exception) {
                            call.respondText(
                                e.message.toString(),
                                status = HttpStatusCode.Created
                            )
                        }
                    }
                }
                if (unsuccessful.size != 0) {
                    call.respondText(
                        "$deletedRow row(s) deleted. Values with ids ${unsuccessful} weren't deleted.",
                        status = HttpStatusCode.Created
                    )
                } else {
                    call.respondText(
                        "$deletedRow row(s) deleted.",
                        status = HttpStatusCode.Created
                    )
                }
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(),
                    status = HttpStatusCode.BadRequest
                )
            }
        }

    }
}