package com.card.terminal.http.plugins

import com.card.terminal.http.routes.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        cardRouting()
        personRouting()
        eventRouting()
        calendarRouting()
        accessLevelRouting()
        controlInterfaceRouting()
        deviceRouting()
    }
}