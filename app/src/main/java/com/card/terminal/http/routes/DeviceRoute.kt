package com.card.terminal.http.routes

import com.card.terminal.database
import com.card.terminal.db.entity.Device
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import timber.log.Timber

fun Route.deviceRouting() {
    route("/device") {
        get {
            Timber.d("Msg: GET request on /device")
            val list = database?.DeviceDao()?.getAll()
            if (list?.isNotEmpty() == true) {
                call.respond(list)
            } else {
                call.respondText("Device db is empty", status = HttpStatusCode.OK)
            }
        }

        get("/{id?}") {
            val id = call.parameters["id"]
            Timber.d("Msg: GET request on /device/$id")
            val ac = id?.let { it1 -> database?.DeviceDao()?.get(it1.toInt()) }
            if (ac != null) {
                call.respond(ac)
            } else {
                call.respondText("No device with id ${id}", status = HttpStatusCode.OK)
            }
        }

        post {
            Timber.d("Msg: POST request on /device")
            val dev = call.receive<Device>()
            try {
                database?.DeviceDao()?.insert(dev)
                call.respondText("Device stored correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        post("/list") {
            Timber.d("Msg: POST request on /device/list")
            val list = call.receive<List<Device>>()
            try {
                database?.DeviceDao()?.insertAll(list)
                call.respondText(
                    "Device list stored correctly",
                    status = HttpStatusCode.Created
                )
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }

        delete {
            Timber.d("Msg: DELETE request on /device")
            val dev = call.receive<Device>()
            try {
                database?.DeviceDao()?.delete(dev)
                call.respondText("Device deleted correctly", status = HttpStatusCode.Created)
            } catch (e: Exception) {
                call.respondText(
                    e.message.toString(), status = HttpStatusCode.BadRequest
                )
            }
        }
    }
}