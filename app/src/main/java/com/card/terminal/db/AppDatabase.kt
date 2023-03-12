package com.card.terminal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.card.terminal.db.dao.*
import com.card.terminal.db.entity.*

@Database(entities = [Card::class, Event::class, Person::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun CardDao(): CardDao
    abstract fun EventDao(): EventDao
    abstract fun PersonDao(): PersonDao
}