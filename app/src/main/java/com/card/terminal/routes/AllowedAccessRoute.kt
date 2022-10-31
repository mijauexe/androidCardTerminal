package com.card.terminal.routes

import com.card.terminal.database
import com.card.terminal.db.entity.AllowedAccess
import com.card.terminal.db.entity.readInfoStorage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.allowedAccessRouting() {
    route("/allowedAccess") {
        get {
            val list = database?.AllowedAccessDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("No customers found", status = HttpStatusCode.OK)
            }
        }
        post {
            var allowedAccess: AllowedAccess? = null
            try {
                allowedAccess = call.receive<AllowedAccess>()
                database?.AllowedAccessDao()?.insertAll(allowedAccess)
                call.respondText("allowedAccess stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                print(e)
            }

        }
        delete("{id?}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            if (readInfoStorage.removeIf { it.uid == Integer.parseInt(id) }) {
                call.respondText("Customer removed correctly", status = HttpStatusCode.Accepted)
            } else {
                call.respondText("Not Found", status = HttpStatusCode.NotFound)
            }
        }
    }
}