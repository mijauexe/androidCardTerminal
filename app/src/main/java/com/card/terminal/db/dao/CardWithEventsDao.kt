package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.card.terminal.db.entity.CardWithEvents

@Dao
interface CardWithEventsDao {
    @Transaction
    @Query("SELECT * FROM Card")
    fun getAll(): List<CardWithEvents>?
}