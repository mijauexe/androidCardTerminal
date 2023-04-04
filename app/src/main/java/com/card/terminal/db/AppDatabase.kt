package com.card.terminal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.card.terminal.db.dao.*
import com.card.terminal.db.entity.*
import timber.log.Timber

@Database(entities = [Card::class, Event::class, Person::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var instance: AppDatabase? = null
        fun getInstance(context: Context): AppDatabase {
            if (instance == null) {
//                instance = Room.databaseBuilder(context,AppDatabase::class.java,"the_database.db")
//                    .allowMainThreadQueries()
//                    .build()

                instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, "AppDatabase"
                ).allowMainThreadQueries().fallbackToDestructiveMigration().build()
            }
            Timber.d("Msg: Instantiating database")
            return instance as AppDatabase
        }
    }

    abstract fun CardDao(): CardDao
    abstract fun EventDao(): EventDao
    abstract fun PersonDao(): PersonDao
    abstract fun PersonWithCardsDao(): PersonWithCardsDao
    abstract fun CardWithEventsDao(): CardWithEventsDao
}