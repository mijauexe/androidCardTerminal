package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.card.terminal.db.entity.Event

@Dao
interface EventDao {
    @Query("SELECT * FROM Event")
    fun getAll(): List<Event>

    @Query("SELECT * FROM Event WHERE card_number = :cardNumber")
    fun getEventsByCardNumber(cardNumber: String): List<Event>

    @Insert
    fun insertAll(vararg event: Event)
}