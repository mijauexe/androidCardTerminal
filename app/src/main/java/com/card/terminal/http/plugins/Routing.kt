package com.card.terminal.http.plugins

import com.card.terminal.http.routes.cardRouting
import com.card.terminal.http.routes.controlInterfaceRouting
import com.card.terminal.http.routes.eventRouting
import com.card.terminal.http.routes.personRouting
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        cardRouting()
        personRouting()
        eventRouting()
        controlInterfaceRouting()
    }
}