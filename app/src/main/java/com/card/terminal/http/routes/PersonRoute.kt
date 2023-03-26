package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Person
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.personRouting() {
    route("/person") {
        get {
            val list = database?.PersonDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Person db is empty", status = HttpStatusCode.OK)
            }
        }

        get("/{id}") {
            val id = call.parameters["id"]
            val person = id?.let { it1 -> database?.PersonDao()?.get(it1.toInt()) }
            if (person != null) {
                call.respond(person)
            } else {
                call.respondText("No person with id ${id}", status = HttpStatusCode.OK)
            }
        }

        post {
            val person = call.receive<Person>()
            try {
                database?.PersonDao()?.insert(person)
                call.respondText("Person stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                print(e.printStackTrace())
                call.respondText(
                    e.printStackTrace().toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/list") {
            val persons = call.receive<List<Person>>()
            try {
                database?.PersonDao()?.insertAll(persons)
                call.respondText("Persons stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                print(e.printStackTrace())
                call.respondText(
                    e.printStackTrace().toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        get("/cards") {
            val list = database?.PersonWithCardsDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Db empty.", status = HttpStatusCode.OK)
            }
        }

        delete("/list") {
            val listOfIdsToBeDeleted = call.receive<Map<String, List<Int>>>()
            var deletedRow = 0
            val unsuccessful = mutableListOf<Int>()
            try {
                for (values in listOfIdsToBeDeleted.values) {
                    for (v in values) {
                        try {
                            val del = database?.PersonDao()?.deleteOne(v)
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