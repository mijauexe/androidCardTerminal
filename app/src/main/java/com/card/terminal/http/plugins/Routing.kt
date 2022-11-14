package com.card.terminal.http.plugins

import com.card.terminal.http.routes.allowedAccessRouting
import com.card.terminal.http.routes.readInfoRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        readInfoRouting()
        allowedAccessRouting()
    }
}