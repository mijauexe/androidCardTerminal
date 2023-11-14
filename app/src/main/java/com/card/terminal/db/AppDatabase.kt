package com.card.terminal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.card.terminal.db.dao.*
import com.card.terminal.db.entity.*

@Database(
    entities = [Card::class, Event::class, Person::class, Calendar::class, AccessLevel::class, Device::class, OperationSchedule::class, OperationMode::class, Button::class],
    version = 17
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private var instance: AppDatabase? = null
        fun getInstance(context: Context, stackTrace: Array<StackTraceElement>): AppDatabase {
            if (instance == null) {

                instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java, "AppDatabase"
                ).allowMainThreadQueries()/*.fallbackToDestructiveMigration()*/.build()

                //TODO MAKNI MAIN THREAD QUERIES...
            }
//            Timber.d("Msg: Instantiating database")
//            Timber.d("Msg: start stacktrace")
//            for (element in stackTrace) {
//                Timber.d("${element.className}.${element.methodName}, ln ${element.lineNumber}")
//            }
//            Timber.d("Msg: end stacktrace")
            return instance as AppDatabase
        }
    }

    abstract fun CardDao(): CardDao
    abstract fun EventDao(): EventDao
    abstract fun PersonDao(): PersonDao
    abstract fun PersonWithCardsDao(): PersonWithCardsDao
    abstract fun CardWithEventsDao(): CardWithEventsDao
    abstract fun CalendarDao(): CalendarDao
    abstract fun AccessLevelDao(): AccessLevelDao
    abstract fun PersonWithAccessLevelsDao(): PersonWithAccessLevelsDao
    abstract fun DeviceDao(): DeviceDao
    abstract fun OperationScheduleDao(): OperationScheduleDao
    abstract fun OperationModeDao(): OperationModeDao
    abstract fun ButtonDao(): ButtonDao
}