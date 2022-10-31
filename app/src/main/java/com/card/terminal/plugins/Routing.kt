package com.card.terminal.plugins

import com.card.terminal.routes.allowedAccessRouting
import com.card.terminal.routes.readInfoRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        readInfoRouting()
        allowedAccessRouting()
    }
}