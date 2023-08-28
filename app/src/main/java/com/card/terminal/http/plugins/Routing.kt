package com.card.terminal.http.plugins

import com.card.terminal.http.routes.accessLevelRouting
import com.card.terminal.http.routes.calendarRouting
import com.card.terminal.http.routes.cardRouting
import com.card.terminal.http.routes.controlInterfaceRouting
import com.card.terminal.http.routes.deviceRouting
import com.card.terminal.http.routes.eventRouting
import com.card.terminal.http.routes.personRouting
import io.ktor.server.application.Application
import io.ktor.server.routing.routing

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