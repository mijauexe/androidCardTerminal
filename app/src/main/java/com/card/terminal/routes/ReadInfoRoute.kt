package com.card.terminal.routes

import com.card.terminal.database
import com.card.terminal.db.entity.ReadInfo
import com.card.terminal.db.entity.readInfoStorage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.readInfoRouting() {
    route("/readInfo") {
        get {
            if (readInfoStorage.isNotEmpty()) {
                call.respond(readInfoStorage)
            } else {
                call.respondText("No customers found", status = HttpStatusCode.OK)
            }
        }
        get("{id?}") {
            val id = call.parameters["id"] ?: return@get call.respondText(
                "Missing id",
                status = HttpStatusCode.BadRequest
            )
            val customer =
                readInfoStorage.find { it.uid == Integer.parseInt(id) }
                    ?: return@get call.respondText(
                        "No customer with id $id",
                        status = HttpStatusCode.NotFound
                    )
            call.respond(customer)
        }
        post {
            val readInfo = call.receive<ReadInfo>()
            database?.ReadInfoDao()?.insertAll(readInfo)
            call.respondText("Customer stored correctly", status = HttpStatusCode.Created)
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