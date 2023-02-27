package com.card.terminal

import com.card.terminal.db.AppDatabase

var database: AppDatabase? = null

//private val server by lazy {
//    embeddedServer(Netty, port = 6969) {
//        configureSerialization()
//        configureRouting()
//    }
//}

fun main(appDatabase: AppDatabase) {
    database = appDatabase
}

//fun stopNetty() {
//    try {
//        if (scope!!.isActive) {
//            server.stop(1000, 2000)
//            scope!!.cancel()
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//}