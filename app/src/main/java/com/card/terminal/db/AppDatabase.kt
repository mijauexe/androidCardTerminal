package com.card.terminal.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.card.terminal.db.dao.AllowedAccessDao
import com.card.terminal.db.dao.PinCodeDao
import com.card.terminal.db.dao.ReadInfoDao
import com.card.terminal.db.entity.AllowedAccess
import com.card.terminal.db.entity.PinCode
import com.card.terminal.db.entity.ReadInfo

@Database(entities = [ReadInfo::class, AllowedAccess::class, PinCode::class], version = 2)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ReadInfoDao(): ReadInfoDao
    abstract fun AllowedAccessDao(): AllowedAccessDao
    abstract fun PinCodeDao(): PinCodeDao
}