package com.card.terminal

import com.card.terminal.db.AppDatabase
import com.card.terminal.plugins.configureRouting
import com.card.terminal.plugins.configureSerialization
import io.ktor.server.engine.*
import io.ktor.server.netty.*

var database: AppDatabase? = null

fun main(appDatabase: AppDatabase) {
    database = appDatabase

    embeddedServer(Netty, port = 6969, host = "0.0.0.0") {
        configureSerialization()
        configureRouting()
    }.start(wait = true)
}