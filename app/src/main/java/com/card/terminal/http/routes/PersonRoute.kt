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
                call.respondText("Person is empty", status = HttpStatusCode.OK)
            }
        }
        post {
            val person = call.receive<Person>()
            database?.PersonDao()?.insertAll(person)
            call.respondText("Persons stored correctly", status = HttpStatusCode.Created)
        }
        delete("{id?}") {

        }
    }
}