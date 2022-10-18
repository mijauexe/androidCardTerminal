package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.card.terminal.db.entity.AllowedAccess

@Dao
interface AllowedAccessDao {
    @Query("SELECT * FROM AllowedAccess")
    fun getAll(): List<AllowedAccess>

    @Insert
    fun insertAll(vararg allowedAccess: AllowedAccess)
}