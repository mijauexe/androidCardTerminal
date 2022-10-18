package com.card.terminal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.card.terminal.db.dao.AllowedAccessDao
import com.card.terminal.db.dao.ReadInfoDao
import com.card.terminal.db.entity.AllowedAccess
import com.card.terminal.db.entity.ReadInfo

@Database(entities = [ReadInfo::class, AllowedAccess::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ReadInfoDao(): ReadInfoDao
    abstract fun AllowedAccessDao(): AllowedAccessDao
}