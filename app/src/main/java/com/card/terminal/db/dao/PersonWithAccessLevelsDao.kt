package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.card.terminal.db.entity.PersonWithAccessLevels

@Dao
interface PersonWithAccessLevelsDao {
    @Transaction
    @Query("SELECT * FROM Person")
    fun getAll(): List<PersonWithAccessLevels>?
}