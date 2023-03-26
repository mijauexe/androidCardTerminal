package com.card.terminal

import com.card.terminal.db.AppDatabase

var database: AppDatabase? = null

fun main(appDatabase: AppDatabase) {
    database = appDatabase
}